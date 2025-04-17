package io.github.makbn.mcp.mediator.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;


@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
public final class McpMediatorDefaultConfigurationBuilder {
    public static final String DEFAULT_SERVER_NAME_KEY = "mcp.mediator.server.name";
    public static final String DEFAULT_SERVER_NAME_VALUE = "mcp_mediator_server";
    public static final String DEFAULT_SERVER_VERSION_KEY = "mcp.mediator.server.version";
    public static final String DEFAULT_SERVER_VERSION_VALUE = "1.0.0";

    McpMediatorDefaultConfiguration configuration;

    @SuppressWarnings("java:S106")
    static McpMediatorDefaultConfigurationBuilder builder() {
        McpMediatorDefaultConfiguration defaultConfiguration = new McpMediatorDefaultConfiguration();

        defaultConfiguration.setServerName(System.getProperty(DEFAULT_SERVER_NAME_KEY, DEFAULT_SERVER_NAME_VALUE));
        defaultConfiguration.setServerVersion(System.getProperty(DEFAULT_SERVER_VERSION_KEY, DEFAULT_SERVER_VERSION_VALUE));
        defaultConfiguration.setSerializer(JsonMapper.builder().build());
        defaultConfiguration.setToolsEnabled(Boolean.TRUE);
        defaultConfiguration.setTransportType(McpTransportType.STDIO);
        defaultConfiguration.setStdioInputStream(System.in);
        defaultConfiguration.setStdioOutputStream(System.out);

        return McpMediatorDefaultConfigurationBuilder.of(defaultConfiguration);
    }

    /**
     * Loads configuration values from an existing {@link McpMediatorConfigurationSpec}.
     *
     * @param config the existing configuration spec
     * @return the builder instance
     */
    @NonNull
    public McpMediatorDefaultConfigurationBuilder from(@NonNull McpMediatorDefaultConfiguration config) {
        this.configuration.setServerName(config.getServerName());
        this.configuration.setServerVersion(config.getServerVersion());
        this.configuration.setToolsEnabled(config.isToolsEnabled());
        this.configuration.setTransportType(config.getTransportType());
        this.configuration.setStdioInputStream(config.getStdioInputStream());
        this.configuration.setStdioOutputStream(config.getStdioOutputStream());
        this.configuration.setSerializer(config.getSerializer());
        this.configuration.setServerAddress(config.getServerAddress());

        return this;
    }

    /**
     * Sets the server name. A Non-Null and Non-Blank value is required.
     */
    @NonNull
    public McpMediatorDefaultConfigurationBuilder serverName(@NonNull String name) {
        this.configuration.setServerName(name);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfigurationBuilder serverVersion(@NonNull String version) {
        this.configuration.setServerVersion(version);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfigurationBuilder tools(boolean enabled) {
        this.configuration.setToolsEnabled(enabled);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfigurationBuilder objectMapper(@NonNull ObjectMapper objectMapper) {
        this.configuration.setSerializer(objectMapper);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfigurationBuilder transportType(@NonNull McpTransportType transportType) {
        this.configuration.setTransportType(transportType);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfigurationBuilder stdioInputStream(@NonNull InputStream stdioInputStream) {
        this.configuration.setStdioInputStream(stdioInputStream);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfigurationBuilder stdioOutputStream(@NonNull OutputStream stdioOutputStream) {
        this.configuration.setStdioOutputStream(stdioOutputStream);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfigurationBuilder serverAddress(@NonNull String serverAddress) {
        this.configuration.setServerAddress(serverAddress);
        return this;
    }

    @NonNull
    public McpMediatorDefaultConfiguration build() {
        verifyConfigurationProperties();
        return configuration;
    }

    private void verifyConfigurationProperties() {
        if (configuration.getServerName().isBlank()) {
            throw new McpMediatorException("serverName is required");
        } else if (configuration.getServerVersion().isBlank()) {
            throw new McpMediatorException("serverVersion is required");
        }

        if (!configuration.isToolsEnabled()) {
            log.warn("MCP Server Tools capability is disabled!");
        }
    }
}