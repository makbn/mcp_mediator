package io.github.makbn.mcp.mediator.api;

/**
 * Interceptor for incoming MCP client requests.
 *
 * @author Matt Akbarian
 */
public interface McpMediatorInterceptor {

    /**
     * Intercepts the call tool request.
     * We pass it as Object to avoid depending on the MCP schema here.
     *
     * @param request the raw MCP CallToolRequest from the client
     * @throws Exception if the request should be aborted
     */
    void intercept(Object request) throws Exception;
}
