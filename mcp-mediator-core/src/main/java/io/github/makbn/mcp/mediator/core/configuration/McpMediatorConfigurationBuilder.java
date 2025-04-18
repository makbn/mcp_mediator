package io.github.makbn.mcp.mediator.core.configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


/**
 * Entry point builder for creating MCP mediator configuration instances.
 * <p>
 * This class provides factory methods to create specific configuration builders,
 * such as {@link McpMediatorDefaultConfigurationBuilder} for standard setups,
 * or {@link McpMediatorProxyConfigurationBuilder} for proxy-based configurations.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * McpMediatorDefaultConfiguration defaultConfig = McpMediatorConfigurationBuilder.builder()
 *     .createDefault() // for DefaultMcpMediator
 *     .serverName("MyServer")
 *     .serverVersion("1.0.0")
 *     .serializer(objectMapper)
 *     .build();
 * }</pre>
 *
 * @see McpMediatorDefaultConfiguration
 * @see McpMediatorProxyConfiguration
 * @see McpMediatorDefaultConfigurationBuilder
 * @see McpMediatorProxyConfigurationBuilder
 *
 * @author Matt Akbarian
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class McpMediatorConfigurationBuilder {

    /**
     * @return a fresh builder entry point
     */
    public static McpMediatorConfigurationBuilder builder() {
        return new McpMediatorConfigurationBuilder();
    }

    /**
     * Loads the default configuration using predefined system properties and default values.
     *
     * @return the builder instance for {@link McpMediatorDefaultConfiguration}
     */
    @NonNull
    public McpMediatorDefaultConfigurationBuilder createDefault() {
        return McpMediatorDefaultConfigurationBuilder.builder();
    }

    /**
     * Loads the default configuration using predefined system properties and default values.
     *
     * @return the builder instance for {@link McpMediatorProxyConfiguration}
     */
    @NonNull
    public McpMediatorProxyConfigurationBuilder creatProxy() {
        return McpMediatorProxyConfigurationBuilder.builder();
    }

}