package io.github.makbn.mcp.mediator.client.request;

import io.github.makbn.mcp.mediator.api.McpRequest;

import java.util.Map;

/**
 * Request to execute a tool on an external MCP client.
 */
public class ClientRequest implements McpRequest {
    private final String clientType;
    private final String tool;
    private final Map<String, Object> params;
    private final Map<String, Object> clientConfig;

    public ClientRequest(String clientType, String tool, Map<String, Object> params, Map<String, Object> clientConfig) {
        this.clientType = clientType;
        this.tool = tool;
        this.params = params;
        this.clientConfig = clientConfig;
    }

    @Override
    public String getType() {
        return "mcp_client";
    }

    @Override
    public String getImplementationName() {
        return "mcp_client";
    }

    /**
     * Gets the type of client to connect to (e.g., "claude_desktop", "cursor", etc.).
     *
     * @return the client type
     */
    public String getClientType() {
        return clientType;
    }

    /**
     * Gets the tool name to execute on the client.
     *
     * @return the tool name
     */
    public String getTool() {
        return tool;
    }

    /**
     * Gets the parameters for the tool.
     *
     * @return the tool parameters
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * Gets the client configuration.
     *
     * @return the client configuration
     */
    public Map<String, Object> getClientConfig() {
        return clientConfig;
    }
} 