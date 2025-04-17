package io.github.makbn.mcp.mediator.api;

/**
 * MCP Built-in Transport Types. MCP includes two standard transport implementations, {@link McpTransportType#STDIO}
 * and {@link McpTransportType#SSE}.
 *
 *
 * @author Matt Akbarian
 * @see <a href="https://modelcontextprotocol.io/docs/concepts/transports">MCP Transport Types</a>
 */
public enum McpTransportType {
    /**
     * The stdio transport enables communication through standard input and output streams.
     * This is particularly useful for local integrations and command-line tools.
     */
    STDIO,

    /**
     * SSE transport enables server-to-client streaming with HTTP POST requests for client-to-server communication.
     */
    SSE
}
