package io.github.makbn.mcp.mediator.api;

/**
 * Exception class for MCP Mediator specific errors.
 */
public class McpMediatorException extends Exception {
    private final McpMediatorStatus status;

    /**
     * Creates a new McpMediatorException with the specified message and status.
     *
     * @param message the detail message
     * @param status the mediator status when the exception occurred
     */
    public McpMediatorException(String message, McpMediatorStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Creates a new McpMediatorException with the specified message, cause, and status.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     * @param status the mediator status when the exception occurred
     */
    public McpMediatorException(String message, Throwable cause, McpMediatorStatus status) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Gets the mediator status when the exception occurred.
     *
     * @return the mediator status
     */
    public McpMediatorStatus getStatus() {
        return status;
    }
} 