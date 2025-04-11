package io.github.makbn.mcp.mediator.integration.client;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.client.connection.ClientConnection;
import io.github.makbn.mcp.mediator.client.connection.impl.ClaudeDesktopConnection;
import io.github.makbn.mcp.mediator.client.request.ClientRequest;
import io.github.makbn.mcp.mediator.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClientIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private McpMediator mediator;

    @Test
    void testClientConnection() {
        ClientConnection connection = new ClaudeDesktopConnection();
        assertNotNull(connection, "Client connection should be created");
        assertEquals("claude-desktop", connection.getClientType(), 
                "Client type should be claude-desktop");
    }

    @Test
    void testClientRequestExecution() {
        Map<String, Object> params = new HashMap<>();
        params.put("test", "value");
        
        ClientRequest request = new ClientRequest(
                "claude-desktop",
                "test-tool",
                params,
                new HashMap<>()
        );

        Object result = mediator.execute(request);
        assertNotNull(result, "Request execution should return a result");
    }

    @Test
    void testClientRequestHandler() {
        ClientRequest request = new ClientRequest(
                "claude-desktop",
                "test-tool",
                new HashMap<>(),
                new HashMap<>()
        );

        assertTrue(mediator.getHandlers().stream()
                .anyMatch(handler -> handler.canHandle(request)),
                "Should find a handler for client request");
    }
} 