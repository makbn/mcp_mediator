package io.github.makbn.mcp.mediator.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for Mcp Mediator proxy mode.
 *
 * @see McpMediatorDefaultConfiguration
 *
 * @author Matt Akbarian rastaghi
 */
@Getter
@SuperBuilder
@ToString
@Setter(AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class McpMediatorProxyConfiguration extends McpMediatorDefaultConfiguration {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(staticName = "of")
    @Setter(AccessLevel.PACKAGE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class McpMediatorRemoteMcpServerConfiguration {
        public static final int TIMEOUT = 30;

        McpTransportType remoteTransportType;
        /**
         * Address is command for {@link McpTransportType#STDIO}
         */
        String remoteServerAddress;
        @Singular
        List<String> remoteServerArgs;
        @Singular
        Map<String, String> remoteServerEnvs;
        @Builder.Default
        Duration remoteServerTimeout = Duration.ofSeconds(TIMEOUT);
        @NonFinal
        ObjectMapper serializer;

        McpMediatorRemoteMcpServerConfiguration copy() {
            return McpMediatorRemoteMcpServerConfiguration.of(
                    this.getRemoteTransportType(),
                    this.getRemoteServerAddress(),
                    this.getRemoteServerArgs() != null
                            ? new ArrayList<>(this.getRemoteServerArgs())
                            : null,
                    this.getRemoteServerEnvs() != null
                            ? Map.copyOf(this.getRemoteServerEnvs())
                            : null,
                    this.getRemoteServerTimeout(),
                    this.getSerializer()
            );
        }

        @NonNull
        public String getCommand() {
            if (remoteTransportType == McpTransportType.STDIO) {
                return remoteServerAddress;
            }
            throw new UnsupportedOperationException("command is not defined in the scope of SSE server");
        }
    }

    @Builder.Default
    List<McpMediatorRemoteMcpServerConfiguration> remoteMcpServerConfigurations = new ArrayList<>();


    public McpMediatorProxyConfiguration(String serverName, String serverVersion, ObjectMapper serializer,
                                         McpTransportType transportType, boolean toolsEnabled, String serverAddress,
                                         InputStream stdioInputStream, OutputStream stdioOutputStream) {
        super(serverName, serverVersion, serializer, transportType, toolsEnabled, serverAddress, stdioInputStream, stdioOutputStream);
    }
}
