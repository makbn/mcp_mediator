package io.github.makbn.mcp.mediator.api;

import javax.xml.transform.Result;

/**
 * Base interface for MCP request handlers.
 * Each specific MCP implementation should implement this interface for their request types.
 *
 * @param <T> the type of request this handler can process
 */
public interface McpRequestHandler<T extends McpRequest> {
    /**
     * Gets the type of request this handler can process.
     *
     * @return the request type
     */
    String getRequestType();

    /**
     * Gets the implementation name this handler is for.
     *
     * @return the implementation name
     */
    String getImplementationName();

    /**
     * Checks if this handler can process the given request.
     *
     * @param request the request to check
     * @return true if this handler can process the request, false otherwise
     */
    boolean canHandle(McpRequest request);

    /**
     * Processes the given request.
     *
     * @param request the request to process
     * @return the result of processing the request
     * @throws McpMediatorException if processing fails
     */
    Result handle(T request) throws McpMediatorException;
} 