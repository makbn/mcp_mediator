package io.github.makbn.mcp.mediator.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter(AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class McpMediatorProxyConfiguration extends McpMediatorDefaultConfiguration {

    @Getter
    @Builder
    @RequiredArgsConstructor(staticName = "of")
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class McpMediatorRemoteMcpServerConfiguration {
        McpTransportType remoteTransportType;
        String remoteServerAddress;
        List<String> remoteServerArgs;
        ObjectMapper serializer;
    }

    List<McpMediatorRemoteMcpServerConfiguration> remoteMcpServerConfigurations;
}
