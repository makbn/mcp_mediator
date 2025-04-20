package io.github.makbn.mcp.mediator.api.feature;

/**
 * The Model Context Protocol (MCP) provides a standardized way for servers to expose resources to clients.
 * Resources allow servers to share data that provides context to language models, such as files, database schemas,
 * or application-specific information. A URI uniquely identifies each resource.
 *
 * @param subscribe whether the client can subscribe to be notified of changes to individual resources.
 * @param listChanged whether the server will emit notifications when the list of available resources changes.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/server/resources">Server Features-Resources</a>
 */
public record McpResources(boolean subscribe, boolean listChanged) implements McpCapability{
    @Override
    public boolean isClientFeature() {
        return false;
    }

    @Override
    public boolean isServerFeature() {
        return true;
    }
}
