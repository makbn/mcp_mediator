package io.github.makbn.mcp.mediator.core.util;


import io.github.makbn.mcp.mediator.api.McpMediatorException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

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
}
