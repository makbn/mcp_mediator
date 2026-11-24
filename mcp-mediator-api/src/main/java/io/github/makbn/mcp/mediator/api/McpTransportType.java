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
    SSE,

    /** MCP streamable HTTP (single endpoint). Mount {@code DefaultMcpMediator#getHttpServlet()} in a servlet container. */
    STREAMABLE_HTTP,

    /** Stateless streamable HTTP (no sessions; tools get no server exchange). Mount {@code getHttpServlet()} at {@code /mcp}. */
    STATELESS_HTTP,

    /** The MCP SDK's "modern" server API (Spring AI 2.1 "Modern" providers); served at {@code /mcp}. */
    MODERN_HTTP
}
