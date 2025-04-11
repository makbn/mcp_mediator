package io.github.makbn.mcp.mediator.client.connection.impl;

import io.github.makbn.mcp.mediator.client.connection.ClientConnection;
import io.github.makbn.mcp.mediator.client.connection.ClientConnectionProvider;

import java.util.Map;

/**
 * Provider for Claude Desktop MCP client connections.
 */
public class ClaudeDesktopConnectionProvider implements ClientConnectionProvider {
    @Override
    public ClientConnection createConnection(Map<String, Object> config) {
        return new ClaudeDesktopConnection(config);
    }

    @Override
    public String getClientType() {
        return "claude_desktop";
    }
} 