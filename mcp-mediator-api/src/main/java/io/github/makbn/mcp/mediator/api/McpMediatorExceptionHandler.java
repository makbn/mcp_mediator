package io.github.makbn.mcp.mediator.api;

/**
 * Handles exceptions thrown during MCP tool execution or request mapping.
 * Consumers can implement this to provide customized error messages or logging.
 */
public interface McpMediatorExceptionHandler {
    /**
     * Handle the exception and return a user-friendly error message that will be sent back to the MCP Client.
     *
     * @param request the request that caused the exception, if available
     * @param exception the thrown exception
     * @return the string message to be returned in the tool result
     */
    String handleException(McpMediatorRequest<?> request, Throwable exception);
}
