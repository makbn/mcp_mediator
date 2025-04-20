package io.github.makbn.mcp.mediator.api.feature;

import jakarta.annotation.Nonnull;

/**
 * The Model Context Protocol (MCP) provides a standardized way for servers to expose prompt templates to clients.
 * Prompts allow servers to provide structured messages and instructions for interacting with language models.
 * Clients can discover available prompts, retrieve their contents, and provide arguments to customize them.
 *
 * @param listChanged indicates whether the server will emit notifications when the list of available prompts changes.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/server/prompts">Server Features - Prompts</a>
 */
public record McpPrompts(boolean listChanged) implements McpCapability {

    @Override
    public boolean isClientFeature() {
        return false;
    }

    @Override
    public boolean isServerFeature() {
        return true;
    }


    @Nonnull
    @Override
    public String getName() {
        return "prompts";
    }
}
