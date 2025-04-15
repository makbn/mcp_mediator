package io.github.makbn.mcp.mediator.api;

/**
 * Interface for listening to MCP Mediator events.
 * Implementations can register to receive notifications about mediator state changes and events.
 *
 * @author Matt Akbarian
 */
public interface McpMediatorListener {
    /**
     * Called when an error occurs in the mediator.
     *
     * @param error the error that occurred
     */
    void onError(McpMediatorException error);

    /**
     * Called when the mediator receives a message.
     *
     * @param message the received message
     */
    void onRequestReceived(String message);

    /**
     * Called when the mediator sends a message.
     *
     * @param message the sent message
     */
    void onResultSent(String message);

    void onResultGenerated(String message);
} 