package io.github.makbn.mcp.mediator.client.handler;

import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpRequest;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.client.request.ClientRequest;
import io.github.makbn.mcp.mediator.client.connection.ClientConnection;
import io.github.makbn.mcp.mediator.client.connection.ClientConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Base MCP client request handler that connects to external MCP clients.
 * Follows the Model Context Protocol specification.
 */
public class ClientMcpRequestHandler implements McpRequestHandler<ClientRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientMcpRequestHandler.class);
    private final ClientConnectionFactory connectionFactory;

    public ClientMcpRequestHandler(ClientConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public String getRequestType() {
        return "mcp_client";
    }

    @Override
    public String getImplementationName() {
        return "mcp_client";
    }

    @Override
    public boolean canHandle(McpRequest request) {
        return request instanceof ClientRequest;
    }

    @Override
    public Object handle(ClientRequest request) throws McpMediatorException {
        try {
            // Get or create connection to the client
            ClientConnection connection = connectionFactory.getConnection(
                request.getClientType(),
                request.getClientConfig()
            );

            // Execute the tool through the client connection
            return connection.executeTool(
                request.getTool(),
                request.getParams()
            );
        } catch (Exception e) {
            throw new McpMediatorException(
                String.format("Failed to execute tool %s on client %s", 
                    request.getTool(), request.getClientType()),
                e);
        }
    }
} 