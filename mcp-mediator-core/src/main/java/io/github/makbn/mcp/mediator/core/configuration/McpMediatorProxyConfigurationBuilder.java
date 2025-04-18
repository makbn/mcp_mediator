package io.github.makbn.mcp.mediator.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;


/**
 * Builder class for constructing instances of {@link McpMediatorProxyConfiguration}.
 * <p>
 * This builder provides a fluent API to configure various aspects of the MCP Mediator Proxy,
 * including server metadata, tools availability, custom serializers, and remote server configurations.
 * </p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * McpMediatorProxyConfiguration config = McpMediatorProxyConfigurationBuilder.builder()
 *     .serverName("MyServer")
 *     .serverVersion("1.0.0")
 *     .tools(true)
 *     .objectMapper(new ObjectMapper())
 *     .addRemoteServer(mcpMediatorRemoteMcpServerConfiguration)
 *     .build();
 * }</pre>
 *
 * @author Matt Akbarian
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
public final class McpMediatorProxyConfigurationBuilder {

    McpMediatorProxyConfiguration configuration;

    public static McpMediatorProxyConfigurationBuilder builder() {
        return McpMediatorProxyConfigurationBuilder.of(new McpMediatorProxyConfiguration());
    }

    /**
     * Loads configuration values from an existing {@link McpMediatorConfigurationSpec}.
     *
     * @param config the existing configuration spec
     * @return the builder instance
     */
    @NonNull
    public McpMediatorProxyConfigurationBuilder from(@NonNull McpMediatorProxyConfiguration config) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    /**
     * Sets the server name. A Non-Null and Non-Blank value is required.
     */
    @NonNull
    public McpMediatorProxyConfigurationBuilder serverName(@NonNull String name) {
        this.configuration.setServerName(name);
        return this;
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder serverVersion(@NonNull String version) {
        this.configuration.setServerVersion(version);
        return this;
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder tools(boolean enabled) {
        this.configuration.setToolsEnabled(enabled);
        return this;
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder objectMapper(@NonNull ObjectMapper objectMapper) {
        this.configuration.setSerializer(objectMapper);
        return this;
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder addRemoteServer(
            @NonNull McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration server) {
        this.configuration.getRemoteMcpServerConfigurations().add(server);
        return this;
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder addRemoteServers(
            @NonNull Collection<McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration> servers) {
        this.configuration.getRemoteMcpServerConfigurations().addAll(servers);
        return this;
    }

    /**
     * Finalizes the configuration and verify the validity of configuration
     *
     * @return the final configuration instance
     */
    @NonNull
    public McpMediatorProxyConfiguration build() {
        this.configuration.setRemoteMcpServerConfigurations(
                Collections.unmodifiableList(this.configuration.getRemoteMcpServerConfigurations()));

        McpMediatorConfigurationVerification.verifyConfigurationProperties(configuration);
        this.configuration.getRemoteMcpServerConfigurations().forEach(server -> {
            McpMediatorConfigurationVerification.verifyMcpMediatorRemoteMcpServerConfiguration(server);
            server.setSerializer(Objects.requireNonNullElse(server.getSerializer(), this.configuration.getSerializer()));
        });
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}