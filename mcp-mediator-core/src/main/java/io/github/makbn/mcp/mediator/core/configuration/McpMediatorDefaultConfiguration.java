package io.github.makbn.mcp.mediator.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.InputStream;
import java.io.OutputStream;

@Getter
@Setter(AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PROTECTED)
public sealed class McpMediatorDefaultConfiguration implements McpMediatorConfigurationSpec permits McpMediatorProxyConfiguration {
    String serverName;
    String serverVersion;
    boolean toolsEnabled;
    ObjectMapper serializer;
    McpTransportType transportType;

    /**
     * Specific to {@link McpTransportType#SSE} transport mode.
     * @see <a href="https://modelcontextprotocol.io/sdk/java/mcp-server#sse-servlet">Server Transport Providers</a>
     */
    String serverAddress;

    /**
     * Specific to {@link McpTransportType#STDIO} transport mode. By default, it should be {@link System#in}.
     * @see <a href="https://modelcontextprotocol.io/sdk/java/mcp-server#stdio">Server Transport Providers</a>
     */
    InputStream stdioInputStream;

    /**
     * Specific to {@link McpTransportType#STDIO} transport mode. By default, it should be {@link System#out}.
     * @see <a href="https://modelcontextprotocol.io/sdk/java/mcp-server#stdio">Server Transport Providers</a>
     */
    OutputStream stdioOutputStream;
}
