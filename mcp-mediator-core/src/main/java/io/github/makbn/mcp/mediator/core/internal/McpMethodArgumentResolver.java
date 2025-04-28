package io.github.makbn.mcp.mediator.core.internal;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("java:S4276")
public class McpMethodArgumentResolver {
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
    
    ObjectMapper objectMapper;


    public Object[] resolveArguments(Method method, Map<String, Object> jsonMap) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = parameter.getName();
            Object rawValue = jsonMap.get(paramName);

            args[i] = convertValue(parameter.getType(), rawValue);
        }

        return args;
    }

    private Object convertValue(Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }
        Function<Object, Object> converter = TYPE_CONVERTERS.get(targetType);
        if (converter != null) {
            return converter.apply(value);
        }

        return objectMapper.convertValue(value, targetType);
    }


    public void registerTypeConverter(Class<?> type, Function<Object, Object> converter) {
        TYPE_CONVERTERS.put(type, converter);
    }

}
