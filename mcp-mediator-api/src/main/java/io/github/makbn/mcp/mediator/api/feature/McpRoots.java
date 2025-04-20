package io.github.makbn.mcp.mediator.api.feature;

import jakarta.annotation.Nonnull;

/**
 * The Model Context Protocol (MCP) provides a standardized way for clients to expose filesystem “roots” to servers.
 * Roots define the boundaries of where servers can operate within the filesystem, allowing them to
 * understand which directories and files they have access to. Servers can request the list of roots from supporting
 * clients and receive notifications when that list changes.
 *
 * @param listChanged indicates whether the client will emit notifications when the list of roots changes.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/client/roots">Client Features - Roots</a>
 *
 * @author Matt Akbarian
 */
public record McpRoots(boolean listChanged) implements McpCapability {

    @Override
    public boolean isClientFeature() {
        return true;
    }

    @Override
    public boolean isServerFeature() {
        return false;
    }

    @Nonnull
    @Override
    public String getName() {
        return "roots";
    }
}
