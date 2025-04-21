package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import io.github.makbn.mcp.mediator.api.feature.McpRoots;
import io.github.makbn.mcp.mediator.api.feature.McpSampling;
import io.github.makbn.mcp.mediator.core.NativeToolAdapter;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class McpRemoteServerToolAdapter {
    McpLifecycleInitializationRequest request;


    @NonNull
    public McpMediatorRemoteMcpServer getRemoteServer() {
        log.debug("connecting to {}", request.getRemoteServer());
        McpSyncClient client = initializeConnection();
        log.debug("loading all the tools provided by {}", request.getRemoteServer());
        return McpMediatorRemoteMcpServer.of(client, client.listTools().tools()
                .stream()
                .map(nativeTool -> NativeToolAdapter.of(nativeTool, request.getRemoteServer().getSerializer()))
                .toList());
    }

    @NonNull
    private synchronized McpSyncClient initializeConnection() {
        try {
            McpSyncClient client = McpClient.sync(createTransportType())
                    .clientInfo(new McpSchema.Implementation(request.getParams().getClientName(),
                            request.getParams().getClientVersion()))
                    .requestTimeout(request.getRemoteServer().getRemoteServerTimeout())
                    .capabilities(createCapabilities())
                    .loggingConsumer(loggingMessageNotification -> {
                        log.debug("Server log received: {}", loggingMessageNotification);
                    })
                    .build();
            McpSchema.InitializeResult result = client.initialize();
            log.debug("connection result for {} \n is: {}", request.getRemoteServer(), result);
            client.setLoggingLevel(McpSchema.LoggingLevel.DEBUG);
            return client;
        } catch (Exception e) {
            log.error("Failed to initialize connection to remote server", e);
            throw new McpMediatorException("RemoteServerToolAdapter failed!", e);
        }
    }

    @NonNull
    private McpSchema.ClientCapabilities createCapabilities() {
        McpSchema.ClientCapabilities.Builder capabilities = McpSchema.ClientCapabilities.builder();
        request.getParams().getCapabilities()
                .forEach(capability -> {
                    if (capability instanceof McpRoots mcpRoots) {
                        capabilities.roots(mcpRoots.listChanged());
                    } else if (capability instanceof McpSampling) {
                        capabilities.sampling();
                    }
                });

        return capabilities.build();
    }

    @NonNull
    private McpClientTransport createTransportType() {
        if (request.getRemoteServer().getRemoteTransportType() == McpTransportType.SSE) {
            return HttpClientSseClientTransport.builder(request.getRemoteServer().getRemoteServerAddress())
                    .objectMapper(request.getRemoteServer().getSerializer())
                    .build();
        } else if (request.getRemoteServer().getRemoteTransportType() == McpTransportType.STDIO) {
            return new StdioClientTransport(createServerParameters(), request.getRemoteServer().getSerializer());
        } else {
            throw new UnsupportedOperationException("transport type is not supported");
        }
    }

    @NonNull
    private ServerParameters createServerParameters() {
        return ServerParameters.builder(request.getRemoteServer().getCommand())
                .args(request.getRemoteServer().getRemoteServerArgs())
                .env(request.getRemoteServer().getRemoteServerEnvs())
                .build();
    }
}
