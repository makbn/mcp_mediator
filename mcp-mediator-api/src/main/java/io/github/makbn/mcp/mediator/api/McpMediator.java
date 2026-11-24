package io.github.makbn.mcp.mediator.api;


import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Main interface for MCP Mediator implementations.
 * This interface defines the core functionality that all MCP mediator implementations must provide.
 *
 * @author Matt Akbarian
 */
public interface McpMediator {

    /**
     * Initializes the mediator with the given configuration.
     *
     * @throws McpMediatorException if initialization fails
     */
    void initialize() throws McpMediatorException;

    /**
     * Registers a request handler with the mediator.
     *
     * @param handler the handler to register
     */
    <T extends McpMediatorRequest<R>, R> void registerHandler(@Nonnull McpMediatorRequestHandler<T, R> handler);

    @SuppressWarnings("rawtypes")
    List<McpMediatorRequestHandler> getHandlers();
    /**
     * Executes a request using the appropriate handler.
     *
     * @param request the request to execute
     * @return the result of executing the request
     * @throws McpMediatorException if execution fails
     */
    <T extends McpMediatorRequest<R>,  R> R execute(T request) throws McpMediatorException;

    /**
     * Registers an annotated object: its {@code @McpTool}, {@code @McpResource}, {@code @McpPrompt} and
     * {@code @McpComplete} methods (the mediator's own annotations or Spring AI's) become MCP features.
     * {@code @McpService} on the class is optional. Completions must be registered before {@link #initialize()}.
     *
     * @param bean the object holding annotated methods
     */
    default void registerBean(@Nonnull Object bean) {
        throw new UnsupportedOperationException("registerBean is not supported by " + getClass().getName());
    }
}
