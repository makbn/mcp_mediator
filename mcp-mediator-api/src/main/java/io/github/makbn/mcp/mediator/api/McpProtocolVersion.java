package io.github.makbn.mcp.mediator.api;

/**
 *
 * The Model Context Protocol uses string-based version identifiers following the format YYYY-MM-DD
 * to indicate the last date backwards incompatible changes were made.
 * One example version can be '2025-03-26'
 * Clients and servers MAY support multiple protocol versions simultaneously,
 * but they MUST agree on a single version to use for the session.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/versioning">Mcp Versioning</a>
 *
 * @param yyyy The year component of the version, e.g., 2025
 * @param MM The month component of the version, e.g., 03
 * @param dd The day component of the version, e.g., 26
 */
public record McpProtocolVersion(String yyyy, String MM, String dd) {
    public static final String MCP_PROTOCOL_VERSION_FORMAT = "%s-%s-%s";

    String getVersion() {
        return String.format(MCP_PROTOCOL_VERSION_FORMAT, yyyy, MM, dd);
    }
}
