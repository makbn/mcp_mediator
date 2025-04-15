package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


/**
 * Builder class for creating instances of {@link McpMediatorConfigurationSpec}.
 * <p>
 * This builder provides methods for configuring the MCP mediator's environment,
 * such as the server name, version, tool capabilities, and serializer.
 * It also supports loading default values from system properties or cloning from
 * an existing configuration.
 * </p>
 *
 * <p>
 * Default values:
 * <ul>
 *     <li>{@code mcp.mediator.server.name} = {@code mcp_mediator_server}</li>
 *     <li>{@code mcp.mediator.server.version} = {@code 1.0.0}</li>
 * </ul>
 * </p>
 *
 * <p>Use {@link #build()} to validate and create a finalized configuration object.</p>
 *
 * @author Matt Akbarian
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class McpMediatorConfigurationBuilder {
    public static final String DEFAULT_SERVER_NAME_KEY = "mcp.mediator.server.name";
    public static final String DEFAULT_SERVER_NAME_VALUE = "mcp_mediator_server";
    public static final String DEFAULT_SERVER_VERSION_KEY = "mcp.mediator.server.version";
    public static final String DEFAULT_SERVER_VERSION_VALUE = "1.0.0";

    ObjectMapper objectMapper;
    String serverName;
    String serverVersion;
    boolean toolsEnabled;

    /**
     * Loads the default configuration using predefined system properties and default values.
     *
     * @return the builder instance
     */
    @NonNull
    public McpMediatorConfigurationBuilder withDefaults() {
        this.serverName = System.getProperty(DEFAULT_SERVER_NAME_KEY, DEFAULT_SERVER_NAME_VALUE);
        this.serverVersion = System.getProperty(DEFAULT_SERVER_VERSION_KEY, DEFAULT_SERVER_VERSION_VALUE);
        this.objectMapper = JsonMapper.builder().build();
        this.toolsEnabled = true;
        return this;
    }

    /**
     * Loads configuration values from an existing {@link McpMediatorConfigurationSpec}.
     *
     * @param config the existing configuration spec
     * @return the builder instance
     */
    @NonNull
    public McpMediatorConfigurationBuilder from(@NonNull McpMediatorConfigurationSpec config) {
        this.serverName = config.getServerName();
        this.serverVersion = config.getServerVersion();
        this.toolsEnabled = config.isToolsEnabled();
        return this;
    }

    /**
     * Sets the server name. A Non-Null and Non-Blank value is required.
     */
    @NonNull
    public McpMediatorConfigurationBuilder serverName(@NonNull String name) {
        this.serverName = name;
        return this;
    }

    @NonNull
    public McpMediatorConfigurationBuilder serverVersion(@NonNull String version) {
        this.serverVersion = version;
        return this;
    }

    @NonNull
    public McpMediatorConfigurationBuilder tools(boolean enabled) {
        this.toolsEnabled = enabled;
        return this;
    }

    @NonNull
    public McpMediatorConfigurationBuilder objectMapper(@NonNull ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    @NonNull
    public McpMediatorConfigurationSpec build() {
        verifyConfigurationProperties();
        return McpMediatorConfiguration.of(serverName, serverVersion, toolsEnabled, objectMapper);
    }

    private void verifyConfigurationProperties() {
        if (serverName == null || serverName.isBlank()) {
            throw new McpMediatorException("serverName is required");
        } else if (serverVersion == null || serverVersion.isBlank()) {
            throw new McpMediatorException("serverVersion is required");
        }

        if (!toolsEnabled) {
            log.warn("MCP Server Tools capability is disabled!");
        }
    }
}