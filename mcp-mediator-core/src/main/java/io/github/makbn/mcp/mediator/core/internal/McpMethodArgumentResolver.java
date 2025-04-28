package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.core.util.McpUtils;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to resolve method arguments dynamically from a {@link Map} of JSON-like input.
 * <p>
 * It supports automatic conversion of common Java types such as primitives, wrappers, and {@link String}.
 * <p>
 * Users can also register custom converters for additional types using {@link #registerTypeConverter(Class, Function)}.
 * <p>
 *
 * <p><strong>Note:</strong> This class is mainly intended for internal framework use
 * where dynamic method invocation based on deserialized JSON is required.
 *
 * @author Matt Akbarian
 */
@SuppressWarnings("java:S4276")
public class McpMethodArgumentResolver {
    /** Registry of type converters for primitives, wrappers, and String. */
    private static final Map<Class<?>, Function<Object, Object>> TYPE_CONVERTERS = new HashMap<>();

    static {
        // Register converters for primitives and wrappers
        TYPE_CONVERTERS.put(int.class, value -> Integer.parseInt(value.toString()));
        TYPE_CONVERTERS.put(Integer.class, value -> Integer.valueOf(value.toString()));
        TYPE_CONVERTERS.put(long.class, value -> Long.parseLong(value.toString()));
        TYPE_CONVERTERS.put(Long.class, value -> Long.valueOf(value.toString()));
        TYPE_CONVERTERS.put(double.class, value -> Double.parseDouble(value.toString()));
        TYPE_CONVERTERS.put(Double.class, value -> Double.valueOf(value.toString()));
        TYPE_CONVERTERS.put(float.class, value -> Float.parseFloat(value.toString()));
        TYPE_CONVERTERS.put(Float.class, value -> Float.valueOf(value.toString()));
        TYPE_CONVERTERS.put(boolean.class, value -> Boolean.parseBoolean(value.toString()));
        TYPE_CONVERTERS.put(Boolean.class, value -> Boolean.valueOf(value.toString()));
        TYPE_CONVERTERS.put(short.class, value -> Short.parseShort(value.toString()));
        TYPE_CONVERTERS.put(Short.class, value -> Short.valueOf(value.toString()));
        TYPE_CONVERTERS.put(byte.class, value -> Byte.parseByte(value.toString()));
        TYPE_CONVERTERS.put(Byte.class, value -> Byte.valueOf(value.toString()));
        TYPE_CONVERTERS.put(char.class, value -> value.toString().charAt(0));
        TYPE_CONVERTERS.put(Character.class, value -> value.toString().charAt(0));
        TYPE_CONVERTERS.put(String.class, Object::toString);
    }

    /**
     * Resolves the arguments for the given method by mapping input JSON keys to method parameters.
     * <p>
     * This method handles automatic type conversion for supported types.
     *
     * @param method  the method for which arguments are to be resolved
     * @param jsonMap a map representing the JSON input
     * @return an array of arguments, ready to be passed to {@link Method#invoke(Object, Object...)}
     */
    public static Object[] resolveArguments(@NonNull Method method, @NonNull Map<String, Object> jsonMap) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = McpUtils.getParameterName(parameter);
            Object rawValue = jsonMap.get(paramName);
            args[i] = convertValue(parameter.getType(), rawValue);
        }
        return args;
    }

    /**
     * Converts a raw value to the target type using registered converters.
     * <p>
     * If no converter is found, it attempts to cast directly.
     *
     * @param targetType the expected parameter type
     * @param value      the raw value to be converted
     * @return the converted value, or {@code null} if the input value is {@code null}
     */
    @Nullable
    private static Object convertValue(Class<?> targetType, @Nullable Object value) {
        if (value == null) {
            return null;
        }
        Function<Object, Object> converter = TYPE_CONVERTERS.get(targetType);
        if (converter != null) {
            return converter.apply(value);
        }

        return targetType.cast(value);
    }

    /**
     * Registers a custom converter for a specific type.
     * <p>
     * This allows users to handle custom or complex types during argument resolution.
     *
     * @param type      the target type
     * @param converter the converter function to transform input into the target type
     */
    public void registerTypeConverter(@NonNull Class<?> type, @NonNull Function<Object, Object> converter) {
        TYPE_CONVERTERS.put(type, converter);
    }

}
