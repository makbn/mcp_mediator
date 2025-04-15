package io.github.makbn.mcp.mediator.api;

/**
 * Exception class for MCP Mediator specific errors.
 *
 * @author Matt Akbarian
 */
public class McpMediatorException extends RuntimeException {
    /**
     * Creates a new McpMediatorException with the specified message and status.
     *
     * @param message the detail message
     */
    public McpMediatorException(String message) {
        super(message);
    }

    /**
     * Creates a new McpMediatorException with the specified message, cause, and status.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public McpMediatorException(String message, Throwable cause) {
        super(message, cause);
    }

} 