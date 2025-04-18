package io.github.makbn.mcp.mediator.core.configuration;

import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpMediatorConfigurationVerification {

    static void verifyConfigurationProperties(@NonNull McpMediatorConfigurationSpec configuration) {
        if (configuration.getServerName().isBlank()) {
            throw new McpMediatorException("serverName is required");
        } else if (configuration.getServerVersion().isBlank()) {
            throw new McpMediatorException("serverVersion is required");
        } else if (configuration.getSerializer() == null) {
            throw new McpMediatorException("serializer is required");
        }

        if (!configuration.isToolsEnabled()) {
            log.warn("MCP Server Tools capability is disabled!");
        }
    }

    static void verifyMcpMediatorRemoteMcpServerConfiguration(
            @NonNull McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration configuration) {
        if (configuration.getRemoteTransportType() == null) {
            throw new McpMediatorException("Remote MCP Server TransportType is required");
        } else if (configuration.getRemoteServerAddress() == null) {
            throw new McpMediatorException("Remote MCP Server Address is required");
        }

        if (configuration.getRemoteServerArgs() == null) {
            log.warn("Remote MCP Server Args is empty for: {}/{}",
                    configuration.getRemoteTransportType(),
                    configuration.getRemoteServerAddress());
        }

        if (configuration.getSerializer() == null) {
            log.warn("Falling back to Mediator's serializer! Remote MCP Server Serializer is not present for: {}/{} ",
                    configuration.getRemoteTransportType(),
                    configuration.getRemoteServerAddress());
        }
    }

}
