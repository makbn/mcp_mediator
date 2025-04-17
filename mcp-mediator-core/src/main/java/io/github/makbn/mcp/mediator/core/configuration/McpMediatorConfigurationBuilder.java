package io.github.makbn.mcp.mediator.core.configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class McpMediatorConfigurationBuilder {


    public static McpMediatorConfigurationBuilder builder() {
        return new McpMediatorConfigurationBuilder();
    }

    /**
     * Loads the default configuration using predefined system properties and default values.
     *
     * @return the builder instance
     */
    @NonNull
    public McpMediatorDefaultConfigurationBuilder createDefault() {
        return McpMediatorDefaultConfigurationBuilder.builder();
    }

    @NonNull
    public McpMediatorProxyConfigurationBuilder creatProxy() {
        return McpMediatorProxyConfigurationBuilder.builder();
    }

}