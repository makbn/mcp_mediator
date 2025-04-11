package io.github.makbn.mcp.mediator.api;

import java.util.List;

/**
 * Main interface for MCP Mediator implementations.
 * This interface defines the core functionality that all MCP mediator implementations must provide.
 */
public interface McpMediator {

    /**
     * Initializes the mediator with the given configuration.
     *
     * @param config The configuration for the mediator
     * @throws McpMediatorException if initialization fails
     */
    void initialize(McpMediatorConfig config) throws McpMediatorException;

    /**
     * Starts the mediator service.
     *
     * @throws McpMediatorException if starting the service fails
     */
    void start() throws McpMediatorException;

    /**
     * Stops the mediator service.
     *
     * @throws McpMediatorException if stopping the service fails
     */
    void stop() throws McpMediatorException;

    /**
     * Checks if the mediator is running.
     *
     * @return true if the mediator is running, false otherwise
     */
    boolean isRunning();

    /**
     * Gets the current status of the mediator.
     *
     * @return the current status
     */
    McpMediatorStatus getStatus();

    /**
     * Registers a listener for mediator events.
     *
     * @param listener the listener to register
     */
    void registerListener(McpMediatorListener listener);

    /**
     * Unregisters a listener from mediator events.
     *
     * @param listener the listener to unregister
     */
    void unregisterListener(McpMediatorListener listener);

    /**
     * Registers a request handler with the mediator.
     *
     * @param handler the handler to register
     */
    void registerHandler(McpRequestHandler<?> handler);

    /**
     * Unregisters a request handler from the mediator.
     *
     * @param handler the handler to unregister
     */
    void unregisterHandler(McpRequestHandler<?> handler);

    /**
     * Gets all registered request handlers.
     *
     * @return list of registered handlers
     */
    List<McpRequestHandler<?>> getHandlers();

    /**
     * Executes a request using the appropriate handler.
     *
     * @param request the request to execute
     * @return the result of executing the request
     * @throws McpMediatorException if execution fails
     */
    Object execute(McpRequest request) throws McpMediatorException;
} 