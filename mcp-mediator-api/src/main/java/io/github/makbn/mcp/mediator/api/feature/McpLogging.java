package io.github.makbn.mcp.mediator.api.feature;

/**
 * The Model Context Protocol (MCP) provides a standardized way for servers to send structured log messages to clients.
 * Clients can control logging verbosity by setting minimum log levels, with servers sending notifications
 * containing severity levels, optional logger names, and arbitrary JSON-serializable data.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-03-26/server/utilities/logging">Mcp Utilities - Logging</a>
 *
 * @author Matt Akbarian
 */
public record McpLogging() implements McpCapability, McpUtility {
    @Override
    public boolean isClientFeature() {
        return false;
    }

    @Override
    public boolean isServerFeature() {
        return true;
    }
}
