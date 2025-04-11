package io.github.makbn.mcp.mediator.api;

/**
 * Represents the current status of the MCP Mediator.
 */
public enum McpMediatorStatus {
    /**
     * The mediator has been created but not initialized.
     */
    CREATED(0),

    /**
     * The mediator is in the process of initializing.
     */
    INITIALIZING(1),

    /**
     * The mediator has been initialized but not started.
     */
    INITIALIZED(2),

    /**
     * The mediator is in the process of starting.
     */
    STARTING(3),

    /**
     * The mediator is running and fully operational.
     */
    RUNNING(4),

    /**
     * The mediator is in the process of stopping.
     */
    STOPPING(5),

    /**
     * The mediator has been stopped.
     */
    STOPPED(6),

    /**
     * The mediator encountered an error and is in an error state.
     */
    ERROR(-1);

    final int code;

    McpMediatorStatus(int code) {
        this.code = code;
    }
}