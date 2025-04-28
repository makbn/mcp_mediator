package io.github.makbn.mcp.mediator.core.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class McpMethodSchemaGenerator {
    private static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(
            String.class, Integer.class, int.class,
            Long.class, long.class, Float.class, float.class,
            Double.class, double.class, Boolean.class, boolean.class
    );

    ObjectMapper mapper;

    @NonNull
    public String generateSchemaForMethod(Method method) throws JsonProcessingException {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.put("id", "urn:jsonschema:" + method.getDeclaringClass().getName().replace('.', ':') + ":" + method.getName());

        ObjectNode propertiesNode = mapper.createObjectNode();
        ArrayNode requiredArray = mapper.createArrayNode();

        for (Parameter parameter : method.getParameters()) {
            ObjectNode paramSchema = describeParameter(parameter);

            propertiesNode.set(parameter.getName(), paramSchema);

            if (isRequired(parameter)) {
                requiredArray.add(parameter.getName());
            }
        }

        schema.set("properties", propertiesNode);
        if (requiredArray.size() > 0) {
            schema.set("required", requiredArray);
        }

        return mapper.writeValueAsString(schema);
    }

    private  ObjectNode describeParameter(Parameter parameter) {
        ObjectNode paramSchema = mapper.createObjectNode();
        Class<?> type = parameter.getType();

        if (isPrimitiveOrWrapper(type)) {
            paramSchema.put("type", mapJavaTypeToJsonType(type));
        } else if (Collection.class.isAssignableFrom(type)) {
            paramSchema.put("type", "array");
            paramSchema.set("items", mapper.createObjectNode().put("type", "object")); // You can dig deeper if needed
        } else if (Map.class.isAssignableFrom(type)) {
            paramSchema.put("type", "object");
        } else {
            // Complex nested type
            paramSchema.put("type", "object");
            paramSchema.set("properties", describeClassProperties(type));
        }

        // Handle validation annotations
        addValidationConstraints(paramSchema, parameter.getAnnotations());

        return paramSchema;
    }

    private  ObjectNode describeClassProperties(Class<?> clazz) {
        ObjectNode propertiesNode = mapper.createObjectNode();
        for (Field field : clazz.getDeclaredFields()) {
            ObjectNode fieldSchema = mapper.createObjectNode();
            Class<?> fieldType = field.getType();

            if (isPrimitiveOrWrapper(fieldType)) {
                fieldSchema.put("type", mapJavaTypeToJsonType(fieldType));
            } else if (Collection.class.isAssignableFrom(fieldType)) {
                fieldSchema.put("type", "array");
                fieldSchema.set("items", mapper.createObjectNode().put("type", "object"));
            } else if (Map.class.isAssignableFrom(fieldType)) {
                fieldSchema.put("type", "object");
            } else {
                // Recursively handle nested classes
                fieldSchema.put("type", "object");
                fieldSchema.set("properties", describeClassProperties(fieldType));
            }

            // Handle validation annotations if any
            addValidationConstraints(fieldSchema, field.getAnnotations());

            propertiesNode.set(field.getName(), fieldSchema);
        }
        return propertiesNode;
    }

    private static void addValidationConstraints(ObjectNode schema, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            switch (annotation.annotationType().getSimpleName()) {
                case "Min":
                    schema.put("minimum", (Long) getAnnotationValue(annotation, "value"));
                    break;
                case "Max":
                    schema.put("maximum", (Long) getAnnotationValue(annotation, "value"));
                    break;
                case "Size":
                    schema.put("minLength", (Integer) getAnnotationValue(annotation, "min"));
                    schema.put("maxLength", (Integer) getAnnotationValue(annotation, "max"));
                    break;
                case "Pattern":
                    schema.put("pattern", (String) getAnnotationValue(annotation, "regexp"));
                    break;
                case "Default":
                    schema.put("default", String.valueOf(getAnnotationValue(annotation, "value")));
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

    private static String mapJavaTypeToJsonType(Class<?> clazz) {
        if (clazz == String.class) return "string";
        if (clazz == Integer.class || clazz == int.class || clazz == Long.class || clazz == long.class)
            return "integer";
        if (clazz == Float.class || clazz == float.class || clazz == Double.class || clazz == double.class)
            return "number";
        if (clazz == Boolean.class || clazz == boolean.class) return "boolean";
        return "string";
    }

    private static Object getAnnotationValue(Annotation annotation, String propertyName) {
        try {
            return annotation.annotationType().getMethod(propertyName).invoke(annotation);
        } catch (Exception e) {
            return null;
        }
    }


}
