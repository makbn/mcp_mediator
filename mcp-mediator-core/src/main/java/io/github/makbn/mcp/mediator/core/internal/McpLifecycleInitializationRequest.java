package io.github.makbn.mcp.mediator.core.internal;


import io.github.makbn.mcp.mediator.api.McpProtocolVersion;
import io.github.makbn.mcp.mediator.api.feature.McpCapability;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorProxyConfiguration;
import lombok.*;
import lombok.experimental.FieldDefaults;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a request to initialize the lifecycle of a Model Context Protocol (MCP) session.
 * This request follows the JSON-RPC protocol and includes specific parameters required to
 * initiate communication between client and server.
 * The initialization phase MUST be the first interaction between client and server.
 *
 * @author Matt Akbarian
 */
@Data
@Builder
@SuppressWarnings("java:S1170")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class McpLifecycleInitializationRequest {
    private static final String INITIALIZATION_METHOD = "initialize";
    private static final String JSON_RPC = "2.0";

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public static class McpLifecycleInitializationRequestParam {
        private static final String CLIENT_NAME_KEY = "name";
        private static final String CLIENT_VERSION_KEY = "version";
        @Getter(value = AccessLevel.PRIVATE)
        Map<String, String> clientInfo = new HashMap<>();
        List<McpCapability> capabilities = new ArrayList<>();

        @Builder
        public McpLifecycleInitializationRequestParam(@Nullable List<McpCapability> capabilities,
                                                      @NonNull String clientName,
                                                      @NonNull String clientVersion) {
            if (capabilities != null) {
                this.capabilities.addAll(capabilities);
            }
            this.clientInfo.put(CLIENT_NAME_KEY, clientName);
            this.clientInfo.put(CLIENT_VERSION_KEY, clientVersion);
        }

        @NonNull
        public final String getClientName() {
            return clientInfo.get(CLIENT_NAME_KEY);
        }

        @NonNull
        public final String getClientVersion() {
            return clientInfo.get(CLIENT_VERSION_KEY);
        }

    }


    final String method = INITIALIZATION_METHOD;
    final String jsonrpc = JSON_RPC;

    int id;
    McpLifecycleInitializationRequestParam params;
    McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration remoteServer;
}
