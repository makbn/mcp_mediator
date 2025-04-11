package io.github.makbn.mcp.mediator.client.connection;

import java.util.Map;

/**
 * Interface for providers that create connections to specific types of MCP clients.
 */
public interface ClientConnectionProvider {
    /**
     * Creates a new connection to a client.
     *
     * @param config the client configuration
     * @return the new connection
     */
    ClientConnection createConnection(Map<String, Object> config);

    /**
     * Gets the client type this provider supports.
     *
     * @return the client type
     */
    String getClientType();
} 