package io.github.makbn.mcp.mediator.api;

import java.util.Map;

/**
 * Configuration interface for MCP Mediator.
 * Implementations should provide specific configuration properties for their respective platforms.
 */
public interface McpMediatorConfig {
    /**
     * Gets the name of the mediator implementation.
     *
     * @return the implementation name
     */
    String getImplementationName();

    /**
     * Gets all configuration properties as a map.
     *
     * @return map of configuration properties
     */
    Map<String, String> getProperties();

    /**
     * Gets a specific configuration property.
     *
     * @param key the property key
     * @return the property value, or null if not found
     */
    String getProperty(String key);

    /**
     * Gets a specific configuration property with a default value.
     *
     * @param key the property key
     * @param defaultValue the default value to return if the property is not found
     * @return the property value, or the default value if not found
     */
    String getProperty(String key, String defaultValue);

    /**
     * Validates the configuration.
     *
     * @throws McpMediatorException if the configuration is invalid
     */
    void validate() throws McpMediatorException;
} 