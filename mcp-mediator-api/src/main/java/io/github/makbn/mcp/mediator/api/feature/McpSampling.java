package io.github.makbn.mcp.mediator.api.feature;

import jakarta.annotation.Nonnull;

/**
 * The Model Context Protocol (MCP) provides a standardized way for servers to request LLM sampling
 * (“completions” or “generations”) from language models via clients. This flow allows clients to maintain
 * control over model access, selection, and permissions while enabling servers to leverage AI capabilities—with no
 * server API keys necessary. Servers can request text, audio, or image-based interactions and optionally include
 * context from MCP servers in their prompts.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/client/sampling">Client Features-Sampling</a>
 *
 * @author Matt Akbarian
 */
public record McpSampling() implements McpCapability {
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
        return "sampling";
    }
}