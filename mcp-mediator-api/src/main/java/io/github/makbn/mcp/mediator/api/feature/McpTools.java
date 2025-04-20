package io.github.makbn.mcp.mediator.api.feature;

/**
 * The Model Context Protocol (MCP) allows servers to expose tools that can be invoked by language models.
 * Tools enable models to interact with external systems, such as querying databases, calling APIs, or
 * performing computations. Each tool is uniquely identified by a name and includes metadata describing its schema.
 *
 * @param listChanged indicates whether the server will emit notifications when the list of available tools changes.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/server/tools">Server Features - Tools</a>
 */
public record McpTools(boolean listChanged) implements McpCapability {
    @Override
    public boolean isClientFeature() {
        return false;
    }

    @Override
    public boolean isServerFeature() {
        return true;
    }
}
