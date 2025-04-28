package io.github.makbn.mcp.mediator.api;

import java.util.Collection;
import java.util.Properties;

/**
 * Base interface for MCP request handlers.
 * Each MCP implementation should implement this interface for their request types.
 *
 * @param <T> the type of request this handler can process
 *
 * @author Matt Akbarian
 */
public interface McpMediatorRequestHandler<T extends McpMediatorRequest<R>, R> {

    /**
     * Gets the implementation name this handler is for.
     *
     * @return the implementation name
     */
    String getName();

    /**
     * Checks if this handler can process the given request.
     *
     * @param request the request to check
     * @return true if this handler can process the request, false otherwise
     */
    boolean canHandle(McpMediatorRequest<?> request);


    Collection<Class<? extends T>> getAllSupportedRequestClass();

    /**
     * Processes the given request.
     *
     * @param request the request to process
     * @return the result of processing the request
     * @throws McpMediatorException if processing fails
     */
    R handle(T request) throws McpMediatorException;


    default Properties getProperties() {
        return new Properties();
    }


} 