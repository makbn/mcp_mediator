package io.github.makbn.mcp.mediator.core.internal;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class McpLifecycleInitializationRequest {
    private static final String INITIALIZATION_METHOD = "initialize";

    static class McpLifecycleInitializationRequestParam {
        private String protocolVersion;
        private Capabilities capabilities;
        private ClientInfo clientInfo;
    }

    @SuppressWarnings("java:S1170")
    final String method = INITIALIZATION_METHOD;

    int id;
    String jsonrpc;


}
