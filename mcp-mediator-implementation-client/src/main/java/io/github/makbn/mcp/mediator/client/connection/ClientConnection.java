package io.github.makbn.mcp.mediator.client.connection;

import java.util.Map;

/**
 * Interface for connecting to external MCP clients.
 */
public interface ClientConnection {
    /**
     * Executes a tool on the connected client.
     *
     * @param tool the tool name to execute
     * @param params the tool parameters
     * @return the tool execution result
     * @throws Exception if the tool execution fails
     */
    Object executeTool(String tool, Map<String, Object> params) throws Exception;

    /**
     * Gets the client type this connection is for.
     *
     * @return the client type
     */
    String getClientType();

    /**
     * Gets the current configuration for this connection.
     *
     * @return the client configuration
     */
    Map<String, Object> getConfig();

    /**
     * Closes the connection to the client.
     */
    void close();
} 