package io.github.makbn.mcp.mediator.core.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import io.github.makbn.mcp.mediator.core.util.McpAnnotations;
import io.github.makbn.mcp.mediator.core.util.McpUtils;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates a JSON Schema representation of a method's parameters.
 * <p>
 * It analyzes parameter types, annotations (like {@code @NotNull}, {@code @Size}, {@code @Min}, etc.),
 * and builds a schema compliant with common JSON Schema standards.
 * <p>
 * This utility is primarily intended for documentation, validation, or dynamic client generation use cases.
 *
 * @author Matt Akbarian
 */
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class McpMethodSchemaGenerator {
    public static final String OBJECT = "object";
    public static final String VALUE = "value";
    public static final String TYPE_KEY = "type";
    public static final String PROPERTIES_KEY = "properties";

    private static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(
            String.class, Integer.class, int.class,
            Long.class, long.class, Float.class, float.class,
            Double.class, double.class, Boolean.class, boolean.class
    );
    public static final String STRING = "string";
    public static final String ITEMS_KEY = "items";
    public static final String ARRAY = "array";
    public static final String ID_KEY = "id";
    public static final String INTEGER = "integer";
    public static final String BOOLEAN = "boolean";
    public static final String NUMBER = "number";
    public static final String REQUIRED_KEY = "required";

    ObjectMapper mapper;
    @NonFinal
    JsonSchemaGenerator schemaGenerator;

    /**
     * Generates a JSON Schema for the provided method's parameters.
     *
     * @param method the method to generate the schema for
     * @return a JSON string representing the schema
     * @throws JsonProcessingException if serialization fails
     */

    @NonNull
    public String generateSchemaForMethod(Method method) throws JsonProcessingException {
        ObjectNode schema = mapper.createObjectNode();
        schema.put(TYPE_KEY, OBJECT);
        // no draft-04 "id": the MCP SDK validator (JSON Schema 2020-12) rejects it

        ObjectNode propertiesNode = mapper.createObjectNode();
        ArrayNode requiredArray = mapper.createArrayNode();

        // Spring AI treats undeclared parameters as required; the mediator's own family keeps "optional unless @NotNull"
        boolean springAiTool = McpAnnotations.find(method, "McpTool").map(McpAnnotations::isSpringAi).orElse(false);
        for (Parameter parameter : method.getParameters()) {
            if (io.github.makbn.mcp.mediator.core.McpCallContext.isInjectable(parameter)) {
                continue;
            }
            ObjectNode paramSchema = describeParameter(parameter);

            propertiesNode.set(McpUtils.getParameterName(parameter), paramSchema);

            McpAnnotations.find(parameter, "McpToolParam").filter(a -> McpAnnotations.bool(a, "header", false)).ifPresent(a -> {
                String type = paramSchema.path(TYPE_KEY).asText();
                if (!List.of(STRING, INTEGER, BOOLEAN).contains(type)) {
                    throw new McpMediatorException("@McpToolParam(header = true) needs a string, integer or boolean parameter: "
                            + parameter + " in " + method);
                }
                paramSchema.put("x-mcp-header", McpUtils.getParameterName(parameter));
            });
            McpAnnotations.find(parameter, "McpToolParam").or(() -> McpAnnotations.find(parameter, "McpArg"))
                    .map(a -> McpAnnotations.str(a, "description", null))
                    .ifPresent(d -> paramSchema.put("description", d));

            if (isRequired(parameter, springAiTool)) {
                requiredArray.add(McpUtils.getParameterName(parameter));
            }
        }

        schema.set(PROPERTIES_KEY, propertiesNode);
        if (!requiredArray.isEmpty()) {
            schema.set(REQUIRED_KEY, requiredArray);
        }

        return mapper.writeValueAsString(schema);
    }

    /**
     * Output schema for a method's return type, or null when the type can't be an MCP object schema
     * (void, primitives, String, collections, Mono/Flux of those).
     */
    @SneakyThrows
    public String generateOutputSchemaForMethod(Method method) {
        Class<?> type = method.getReturnType();
        if (reactor.core.publisher.Mono.class.isAssignableFrom(type) && method.getGenericReturnType()
                instanceof java.lang.reflect.ParameterizedType pt && pt.getActualTypeArguments()[0] instanceof Class<?> c) {
            type = c;
        }
        if (type == void.class || type == Void.class || type.isPrimitive() || type.isEnum() || type.isArray()
                || isPrimitiveOrWrapper(type) || Collection.class.isAssignableFrom(type)
                || reactor.core.publisher.Flux.class.isAssignableFrom(type) || type == Object.class
                || reactor.core.publisher.Mono.class.isAssignableFrom(type)) {
            return null;
        }
        ObjectNode node = mapper.valueToTree(getSchemaGenerator().generateSchema(type));
        node.remove(ID_KEY);
        return mapper.writeValueAsString(node);
    }

    /**
     * Describes a method parameter and generates its corresponding schema.
     *
     * @param parameter the method parameter
     * @return the JSON schema node describing the parameter
     */
    @SneakyThrows
    @NonNull
    private ObjectNode describeParameter(Parameter parameter) {
        ObjectNode paramSchema = mapper.createObjectNode();
        Class<?> type = parameter.getType();

        if (isPrimitiveOrWrapper(type)) {
            paramSchema.put(TYPE_KEY, mapJavaTypeToJsonType(type));
        } else if (Collection.class.isAssignableFrom(type)) {
            paramSchema.put(TYPE_KEY, ARRAY);
            paramSchema.set(ITEMS_KEY, itemSchema(parameter.getParameterizedType()));
        } else if (Map.class.isAssignableFrom(type)) {
            paramSchema.put(TYPE_KEY, OBJECT);
        } else {
            // Complex nested type
            JsonSchema schema = getSchemaGenerator().generateSchema(type);
            paramSchema = mapper.valueToTree(schema);
            paramSchema.remove(ID_KEY);
        }

        // Handle validation annotations
        addValidationConstraints(paramSchema, parameter.getAnnotations());

        return paramSchema;
    }


    /** Schema of a collection's element type; unknown/wildcard element types accept anything. */
    @SneakyThrows
    private ObjectNode itemSchema(java.lang.reflect.Type collectionType) {
        if (collectionType instanceof java.lang.reflect.ParameterizedType pt
                && pt.getActualTypeArguments().length == 1
                && pt.getActualTypeArguments()[0] instanceof Class<?> element) {
            if (isPrimitiveOrWrapper(element)) {
                return mapper.createObjectNode().put(TYPE_KEY, mapJavaTypeToJsonType(element));
            }
            if (!Collection.class.isAssignableFrom(element) && !Map.class.isAssignableFrom(element)
                    && element != Object.class) {
                ObjectNode node = mapper.valueToTree(getSchemaGenerator().generateSchema(element));
                node.remove(ID_KEY);
                return node;
            }
        }
        return mapper.createObjectNode(); // any
    }

    private JsonSchemaGenerator getSchemaGenerator() {
        if (schemaGenerator == null) {
            schemaGenerator = new JsonSchemaGenerator(mapper);
        }
        return schemaGenerator;
    }

    /**
     * Adds validation constraints extracted from annotations to a given schema node.
     *
     * @param schema      the schema node to augment
     * @param annotations the annotations to analyze
     */
    private static void addValidationConstraints(ObjectNode schema, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            switch (annotation.annotationType().getSimpleName()) {
                case "Min":
                    schema.put("minimum", (Long) getAnnotationValue(annotation, VALUE));
                    break;
                case "Max":
                    schema.put("maximum", (Long) getAnnotationValue(annotation, VALUE));
                    break;
                case "Size":
                    schema.put("minLength", (Integer) getAnnotationValue(annotation, "min"));
                    schema.put("maxLength", (Integer) getAnnotationValue(annotation, "max"));
                    break;
                case "Pattern":
                    schema.put("pattern", (String) getAnnotationValue(annotation, "regexp"));
                    break;
                case "Default":
                    schema.put("default", String.valueOf(getAnnotationValue(annotation, VALUE)));
                    break;
                case "Schema":
                    schema.put("description", (String) getAnnotationValue(annotation, "description"));
                    break;
                // Add more known validations easily
                default:
                    break;
            }
        }
    }

    private static boolean isRequired(Parameter parameter, boolean springAiTool) {
        var declared = McpAnnotations.find(parameter, "McpToolParam").or(() -> McpAnnotations.find(parameter, "McpArg"));
        if (declared.isPresent()) {
            return McpAnnotations.bool(declared.get(), "required", true);
        }
        for (Annotation annotation : parameter.getAnnotations()) {
            // by name: lombok is compile-only and javax/jakarta validation may be absent at runtime
            String n = annotation.annotationType().getSimpleName();
            if (n.equals("NotNull") || n.equals("NonNull") || n.equals("NotBlank") || n.equals("NotEmpty")) {
                return true;
            }
            if (n.equals("Nullable")) {
                return false;
            }
        }
        return springAiTool;
    }

    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return PRIMITIVE_TYPES.contains(clazz);
    }

    /**
     * Maps a Java type to a corresponding JSON Schema type.
     *
     * @param clazz the Java class
     * @return the JSON type as a string
     */
    private static String mapJavaTypeToJsonType(Class<?> clazz) {
        if (clazz == String.class) return STRING;
        if (clazz == Integer.class || clazz == int.class || clazz == Long.class || clazz == long.class)
            return INTEGER;
        if (clazz == Float.class || clazz == float.class || clazz == Double.class || clazz == double.class)
            return NUMBER;
        if (clazz == Boolean.class || clazz == boolean.class) return BOOLEAN;
        return STRING;
    }

    /**
     * Extracts the value of a property from an annotation via reflection.
     *
     * @param annotation   the annotation instance
     * @param propertyName the name of the property to extract
     * @return the extracted value, or {@code null} if inaccessible
     */
    private static Object getAnnotationValue(Annotation annotation, String propertyName) {
        try {
            return annotation.annotationType().getMethod(propertyName).invoke(annotation);
        } catch (Exception e) {
            return null;
        }
    }
}
