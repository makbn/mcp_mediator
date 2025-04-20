package io.github.makbn.mcp.mediator.api.feature;

/**
 * Features and utilities provided by MCP servers and clients.
 *
 * @author Matt Akbarian
 */
public interface McpCapability {

    boolean isClientFeature();
    boolean isServerFeature();
}
