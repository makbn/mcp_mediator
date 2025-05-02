package io.github.makbn.mcp.mediator.core.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import io.github.makbn.mcp.mediator.core.util.McpUtils;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
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
        schema.put(ID_KEY, ("urn:jsonschema:" + method.getDeclaringClass()
                .getName().replace('.', ':') + ":" + method.getName()).toLowerCase());

        ObjectNode propertiesNode = mapper.createObjectNode();
        ArrayNode requiredArray = mapper.createArrayNode();

        for (Parameter parameter : method.getParameters()) {
            ObjectNode paramSchema = describeParameter(parameter);

            propertiesNode.set(McpUtils.getParameterName(parameter), paramSchema);

            if (isRequired(parameter)) {
                requiredArray.add(parameter.getName());
            }
        }

        schema.set(PROPERTIES_KEY, propertiesNode);
        if (!requiredArray.isEmpty()) {
            schema.set(REQUIRED_KEY, requiredArray);
        }

        return mapper.writeValueAsString(schema);
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
            paramSchema.set(ITEMS_KEY, mapper.createObjectNode().put(TYPE_KEY, OBJECT)); // You can dig deeper if needed
        } else if (Map.class.isAssignableFrom(type)) {
            paramSchema.put(TYPE_KEY, OBJECT);
        } else {
            // Complex nested type
            paramSchema.put(TYPE_KEY, OBJECT);
            JsonSchema schema = getSchemaGenerator().generateSchema(type);
            paramSchema.put(ID_KEY, schema.getId());
            paramSchema.set(PROPERTIES_KEY, mapper.valueToTree(schema.asObjectSchema().getProperties()));
        }

        // Handle validation annotations
        addValidationConstraints(paramSchema, parameter.getAnnotations());

        return paramSchema;
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

    private static boolean isRequired(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation instanceof NotNull || annotation instanceof NonNull) {
                return true;
            }
        }
        return false;
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
