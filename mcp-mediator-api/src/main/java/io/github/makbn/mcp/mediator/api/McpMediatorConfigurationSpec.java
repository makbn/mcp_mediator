package io.github.makbn.mcp.mediator.api;

import jakarta.annotation.Nonnull;

/**
 * Specifies the configuration contract for the MCP Mediator.
 * <p>
 * Implementations of this interface should provide the necessary
 * configuration details such as serializer, server name/version,
 * and tooling availability and proper validation for MCP server specification.
 * </p>
 *
 * @author Matt Akbarian
 */
public interface McpMediatorConfigurationSpec {
    /**
     * Returns the serializer instance used by the MCP Mediator.
     *
     * @param <S> the type of the serializer. Can be ObjectMapper or any other compatible libraries.
     * @return the serializer instance
     */
    <S> S getSerializer();

    /**
     * Returns the name of the server.
     * <p>
     * This name is typically used for identification or logging.
     * </p>
     *
     * @return the server name
     */
    @Nonnull
    String getServerName();

    /**
     * Returns the version of the server.
     * <p>
     * Useful for diagnostics, compatibility checks, or display purposes.
     * </p>
     *
     * @return the server version
     */
    @Nonnull
    String getServerVersion();

    @Nonnull
    McpTransportType getTransportType();

    /**
     * Indicates whether the Tool capability of your MCP server is available.
     *
     * @see <a href="https://modelcontextprotocol.io/docs/concepts/tools">MCP Tools Concept</a>
     *
     * @return {@code true} if tools are enabled, {@code false} otherwise
     */
    boolean isToolsEnabled();

    /**
     * Indicates whether the Prompts capability of your MCP server is available.
     * @return {@code true} if tools are enabled, {@code false} otherwise
     */
    default boolean isPromptEnabled() {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Indicates whether the Resource capability of your MCP server is available.
     *
     * @see <a href="https://modelcontextprotocol.io/docs/concepts/resources">MCP Resouerces Concept</a>
     *
     * @return {@code true} if tools are enabled, {@code false} otherwise
     */
    default boolean isResourceEnabled() {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Indicates whether the Sampling capability of your MCP server is available.
     *
     * @see <a href="https://modelcontextprotocol.io/docs/concepts/sampling">MCP Sampling Concept</a>
     *
     * @return {@code true} if tools are enabled, {@code false} otherwise
     */
    default boolean isSamplingEnabled() {
        throw  new UnsupportedOperationException("Not supported yet.");
    }
}
