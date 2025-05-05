package io.github.makbn.mcp.mediator.core.internal;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.concurrent.Callable;

/**
 * Abstract base class for executing a request within a {@link Callable} context.
 * <p>
 * This executor is designed to wrap the logic required to process an {@link io.github.makbn.mcp.mediator.api.McpMediatorRequest}
 * in a controlled and thread-safe environment (typically using an {@link java.util.concurrent.ExecutorService}).
 * <p>
 * Subclasses must implement the {@link #call()} method to provide the actual execution logic.
 *
 * @param <T> the result type returned by this executor's {@code call} method
 *
 * @see java.util.concurrent.Callable
 * @see java.util.concurrent.ExecutorService
 * @see io.github.makbn.mcp.mediator.api.McpMediatorRequest
 *
 * @author Matt Akbarian
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class McpRequestExecutor<T> implements Callable<T> {

}
