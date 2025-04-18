package io.github.makbn.mcp.mediator.core.util;

import lombok.SneakyThrows;

import java.util.function.Function;


/**
 * A {@link Function} variant that allows for throwing checked exceptions.
 * <p>
 * This functional interface is useful when you want to use lambda expressions or method references
 * that throw checked exceptions in a context that expects a standard {@link Function}, such as in streams or optionals.
 * </p>
 *
 * <p>The {@link #apply(Object)} method is overridden to call {@link #sneakyApply(Object)} while suppressing
 * the checked exception requirement using {@link SneakyThrows}.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * Optional.ofNullable(request.getDeclaredAnnotation(McpTool.class))
 *     .map(McpTool::schema)
 *     .map(schema -> McpObjects.sneakyOperation(
 *         (SneakyFunction<Class<?>, JsonSchema>) schemaGenerator::generateSchema, schema))
 *     .map(schema -> McpObjects.sneakyOperation(
 *         (SneakyFunction<JsonSchema, String>) objectMapper::writeValueAsString, schema))
 *     .orElseThrow(() -> new McpMediatorException("McpTool annotation not found"));
 * }</pre>
 *
 * @param <T> the input type
 * @param <R> the result type
 *
 * @author Matt Akbarian
 */
@FunctionalInterface
public interface SneakyFunction<T, R> extends Function<T, R> {


    /**
     * Applies this function to the given argument, allowing for a checked exception to be thrown.
     *
     * @param t the function argument
     * @return the function result
     * @throws Exception if a generic error occurs during execution which we can not be aware of its type
     */
    @SuppressWarnings("java:S112")
    R sneakyApply(T t) throws Exception;


    /**
     * Applies this function to the given argument.
     * Internally delegates to {@link #sneakyApply(Object)} and suppresses checked exceptions.
     *
     * @param t the function argument
     * @return the function result
     */
    @Override
    @SneakyThrows
    default R apply(T t) {
        return sneakyApply(t);
    }
}
