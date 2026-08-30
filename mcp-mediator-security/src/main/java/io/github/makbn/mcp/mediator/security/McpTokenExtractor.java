package io.github.makbn.mcp.mediator.security;

/**
 * Strategy interface to extract an authentication token from the raw MCP request.
 */
public interface McpTokenExtractor {
    /**
     * @param request the incoming MCP request, typically McpSchema.CallToolRequest
     * @return the extracted McpAuthentication, or null if no token is found
     * @throws Exception if extraction fails due to invalid format
     */
    McpAuthentication extract(Object request) throws Exception;
}
