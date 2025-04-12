package io.github.makbn.mcp.mediator.api;

import java.util.List;


/**
 * Base interface for all MCP requests.
 * Each specific MCP implementation should extend this interface with their own request types.
 */
public interface McpRequest<T> {
    /**
     * Gets the type of the request.
     *
     * @return the request type
     */
    String getMethod();

    /**
     * Gets the implementation name this request is intended for.
     *
     * @return the implementation name
     */
    List<Object> getParameters();
} 