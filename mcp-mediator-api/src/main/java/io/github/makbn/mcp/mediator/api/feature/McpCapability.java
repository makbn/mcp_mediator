package io.github.makbn.mcp.mediator.api.feature;

import jakarta.annotation.Nonnull;

/**
 * Features and utilities provided by MCP servers and clients.
 *
 * @author Matt Akbarian
 */
public interface McpCapability {
    @Nonnull String getName();
    boolean isClientFeature();
    boolean isServerFeature();
}
