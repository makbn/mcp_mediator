package io.github.makbn.mcp.mediator.client.connection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing connections to external MCP clients.
 */
public class ClientConnectionFactory {
    private final Map<String, ClientConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, ClientConnectionProvider> providers = new ConcurrentHashMap<>();

    /**
     * Registers a provider for a specific client type.
     *
     * @param clientType the client type
     * @param provider the connection provider
     */
    public void registerProvider(String clientType, ClientConnectionProvider provider) {
        providers.put(clientType, provider);
    }

    /**
     * Gets or creates a connection to a client.
     *
     * @param clientType the client type
     * @param config the client configuration
     * @return the client connection
     * @throws IllegalArgumentException if no provider is registered for the client type
     */
    public ClientConnection getConnection(String clientType, Map<String, Object> config) {
        String connectionKey = createConnectionKey(clientType, config);
        return connections.computeIfAbsent(connectionKey, key -> {
            ClientConnectionProvider provider = providers.get(clientType);
            if (provider == null) {
                throw new IllegalArgumentException(
                    String.format("No provider registered for client type: %s", clientType));
            }
            return provider.createConnection(config);
        });
    }

    /**
     * Closes all connections.
     */
    public void closeAll() {
        connections.values().forEach(ClientConnection::close);
        connections.clear();
    }

    /**
     * Creates a unique key for a connection based on client type and configuration.
     *
     * @param clientType the client type
     * @param config the client configuration
     * @return the connection key
     */
    private String createConnectionKey(String clientType, Map<String, Object> config) {
        return clientType + ":" + config.hashCode();
    }
} 