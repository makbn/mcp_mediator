package io.github.makbn.mcp.mediator.api;

import jakarta.annotation.Nonnull;

/**
 * An adapter interface that extracts the structure and metadata for a source tool
 * that can be used by MCP mediator.
 *
 * @param <T> the type representing the original source tool
 *
 * @author Matt Akbarian
 */
public interface McpToolAdapter<T> {

    /**
     * Gets the type of the request.
     *
     * @return the request type.
     */
    @Nonnull String getMethod();

    /**
     * @return  the additional information about the tool being provided to the MCP client.
     */
    @Nonnull String getAnnotations();

    /**
     * Retrieves the description of the request tool.
     * <p>
     * This description provides context or documentation for the tool associated with the request.
     * </p>
     *
     * @return the description of the tool
     */
    @Nonnull String getDescription();
    /**
     * Converts the tool input parameter type as the MCP Server schema.
     *
     * @return the JSON string of the input schema.
     */
    @Nonnull String getSchema();

    /**
     *
     * @return the source tool to be extracted information from
     */
    @Nonnull T getSourceTool();
}
