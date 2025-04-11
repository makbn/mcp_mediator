package io.github.makbn.mcp.mediator.api;

/**
 * Base interface for all MCP requests.
 * Each specific MCP implementation should extend this interface with their own request types.
 */
public interface McpRequest {
    /**
     * Gets the type of the request.
     *
     * @return the request type
     */
    String getType();

    /**
     * Gets the implementation name this request is intended for.
     *
     * @return the implementation name
     */
    String getImplementationName();
} 