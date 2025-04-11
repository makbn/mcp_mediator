package io.github.makbn.mcp.mediator.client.connection.impl;

import io.github.makbn.mcp.mediator.client.connection.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Connection to Claude Desktop MCP client.
 */
public class ClaudeDesktopConnection implements ClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ClaudeDesktopConnection.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final Map<String, Object> config;

    public ClaudeDesktopConnection(Map<String, Object> config) {
        this.baseUrl = (String) config.getOrDefault("baseUrl", DEFAULT_BASE_URL);
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Object executeTool(String tool, Map<String, Object> params) throws Exception {
        LOG.info("Executing tool {} on Claude Desktop", tool);

        // Create the request body
        String requestBody = createRequestBody(tool, params);

        // Create the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/tools/execute"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        // Send the request and get the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                String.format("Failed to execute tool: %s (status: %d)", 
                    response.body(), response.statusCode()));
        }

        return response.body();
    }

    @Override
    public String getClientType() {
        return "claude_desktop";
    }

    @Override
    public Map<String, Object> getConfig() {
        return config;
    }

    @Override
    public void close() {
        // Nothing to close for HTTP client
    }

    private String createRequestBody(String tool, Map<String, Object> params) {
        // TODO: Implement proper JSON serialization
        return String.format("{\"tool\":\"%s\",\"params\":%s}", tool, params);
    }
} 