package io.github.makbn.mcp.mediator.core.util;


import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.feature.McpArgument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.function.Function;


/**
 * Utility class for common object-related operations within the MCP mediator context.
 * <p>
 * This class provides helper methods for unchecked casting and function execution that may throw exceptions.
 * </p>
 *
 * @author Matt Akbarion
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpUtils {

    /**
     * Performs an unchecked cast of an object to a specified type.
     * <p>
     * Use this method with caution. If the object is not of the expected type,
     * a {@link ClassCastException} will be caught and rethrown as an {@link McpMediatorException}.
     * </p>
     *
     * @param object the object to cast
     * @param <T>    the target type
     * @return the casted object, or {@code null} if the input is {@code null}
     * @throws McpMediatorException if the cast fails
     */
    @SuppressWarnings({"unchecked", "java:S1854"})
    public static <T> T sneakyCast(Object object) {
        if (object == null) {
            return null;
        } else {
            try {
                return  (T) object;
            }catch (ClassCastException e) {
                throw new McpMediatorException("Failed to cast the result", e);
            }
        }
    }

    /**
     * Executes a function that may throw a checked exception, suppressing the need to declare it.
     * <p>
     * This is useful for wrapping operations (such as serialization) that throw checked exceptions,
     * allowing for cleaner functional-style code.
     * </p>
     *
     * @param writeValueAsString the function to apply
     * @param schema             the input to the function
     * @param <T>                the input type
     * @param <R>                the return type
     * @return the result of the function
     * @throws RuntimeException if the function throws a checked exception
     */
    @SneakyThrows
    public static <T, R> R sneakyOperation(Function<T, R> writeValueAsString, T schema) {
        return writeValueAsString.apply(schema);
    }

    /**
     * Converts a given camelCase string to snake_case.
     * @throws NullPointerException if {@code input} is {@code null}
     */
    @NonNull
    public static String convertCamelCaseToSnake(@NonNull String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append("_").append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Retrieves the name of the given method parameter.
     * <p>
     * If the parameter is annotated with {@link io.github.makbn.mcp.mediator.api.feature.McpArgument},
     * the annotation's {@code name} value is returned. Otherwise, the actual parameter name
     * as preserved by the Java compiler is used.
     * <p>
     * <strong>Important:</strong>
     * <ul>
     *     <li>By default, Java replaces method parameter names unless the
     *     {@code -parameters} compiler flag is used during compilation.</li>
     *     <li>Using the {@link McpArgument} annotation ensures the parameter name
     *     is reliably available through reflection even if the flag is not set.</li>
     * </ul>
     *
     * @param parameter the {@link Parameter} whose name is to be retrieved
     * @return the parameter name, either from {@link McpArgument} or the actual reflection name
     * @throws NullPointerException if {@link McpArgument} is present but its {@code name} is {@code null}
     */
    @NonNull
    public static String getParameterName(@NonNull Parameter parameter) {
        if (parameter.isAnnotationPresent(McpArgument.class)) {
            return Objects.requireNonNull(parameter.getAnnotation(McpArgument.class)).name();
        }
        return parameter.getName();
    }
}
