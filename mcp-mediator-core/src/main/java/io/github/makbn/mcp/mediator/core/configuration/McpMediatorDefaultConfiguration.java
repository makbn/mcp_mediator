package io.github.makbn.mcp.mediator.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Default base implementation of {@link McpMediatorConfigurationSpec} that provides common configuration
 * options shared across different mediator implementations.
 * <p>
 * This class is intended to be extended by specific configuration types like {@link McpMediatorProxyConfiguration},
 * and supports multiple transport modes such as {@link McpTransportType#STDIO} and {@link McpTransportType#SSE}.
 * </p>
 *
 * <p>
 * Depending on the selected {@link #transportType}, specific fields (like {@link #serverAddress},
 * {@link #stdioInputStream}, and {@link #stdioOutputStream}) will be relevant.
 * </p>
 *
 * @see McpTransportType
 * @see McpMediatorProxyConfiguration
 * @see McpMediatorConfigurationSpec
 *
 * @author Matt Akbarian
 */
@Getter
@Setter(AccessLevel.PACKAGE)
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PROTECTED)
public sealed class McpMediatorDefaultConfiguration implements McpMediatorConfigurationSpec permits McpMediatorProxyConfiguration {
    @Builder.Default
    String serverName = "";
    @Builder.Default
    String serverVersion = "";
    @Builder.Default
    ObjectMapper serializer = JsonMapper.builder().build();
    @Builder.Default
    McpTransportType transportType = McpTransportType.STDIO;
    @Builder.Default
    boolean toolsEnabled = true;

    /**
     * Specific to {@link McpTransportType#SSE} transport mode.
     * @see <a href="https://modelcontextprotocol.io/sdk/java/mcp-server#sse-servlet">Server Transport Providers</a>
     */
    String serverAddress;

    /**
     * Specific to {@link McpTransportType#STDIO} transport mode. By default, it should be {@link System#in}.
     * @see <a href="https://modelcontextprotocol.io/sdk/java/mcp-server#stdio">Server Transport Providers</a>
     */
    @Builder.Default
    InputStream stdioInputStream = System.in;

    /**
     * Specific to {@link McpTransportType#STDIO} transport mode. By default, it should be {@link System#out}.
     * @see <a href="https://modelcontextprotocol.io/sdk/java/mcp-server#stdio">Server Transport Providers</a>
     */
    @Builder.Default
    OutputStream stdioOutputStream = System.out;
}
