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


/**
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
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder serverVersion(@NonNull String version) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder tools(boolean enabled) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder objectMapper(@NonNull ObjectMapper objectMapper) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @NonNull
    public McpMediatorProxyConfiguration build() {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}