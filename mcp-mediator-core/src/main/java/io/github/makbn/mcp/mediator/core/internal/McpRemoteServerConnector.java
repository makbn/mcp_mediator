package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import io.github.makbn.mcp.mediator.api.feature.McpRoots;
import io.github.makbn.mcp.mediator.api.feature.McpSampling;
import io.github.makbn.mcp.mediator.core.adaper.NativeToolAdapter;
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

/**
 * Responsible for establishing a connection to a remote MCP (Model Context Protocol) server,
 * initializing the connection, and adapting its available tools into the MCP Mediator format.
 * <p>
 * This class supports multiple transport types (e.g., SSE, STDIO), declares supported client capabilities,
 * and provides access to remote tools in a structured form.
 *
 * @author Matt Akbarian
 */
@Slf4j
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class McpRemoteServerConnector {
    McpLifecycleInitializationRequest request;

    /**
     * Connects to the remote MCP server and loads all available tools.
     *
     * @return a {@link McpMediatorRemoteMcpServer} containing wrapped tools
     * @throws McpMediatorException if connection or tool loading fails
     */
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

    /**
     * Initializes a synchronous MCP client connection using the provided request parameters.
     * Configures transport, client info, timeouts, capabilities, and logging.
     *
     * @return an initialized {@link McpSyncClient}
     * @throws McpMediatorException if initialization fails
     */
    @NonNull
    private synchronized McpSyncClient initializeConnection() {
        try {
            McpSyncClient client = McpClient.sync(createTransportType())
                    .clientInfo(new McpSchema.Implementation(request.getParams().getClientName(),
                            request.getParams().getClientVersion()))
                    .requestTimeout(request.getRemoteServer().getRemoteServerTimeout())
                    .capabilities(createCapabilities())
                    .loggingConsumer(loggingMessageNotification ->
                            log.debug("Server log received: {}", loggingMessageNotification))
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

    /**
     * Constructs and declares the set of client capabilities supported by this connector.
     * Currently, supports {@link McpRoots} and {@link McpSampling}.
     *
     * @return a configured {@link McpSchema.ClientCapabilities} object
     */
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

    /**
     * Creates the appropriate transport instance based on the requested transport type
     * (e.g., SSE or STDIO).
     *
     * @return a valid {@link McpClientTransport} for use with the MCP client
     * @throws UnsupportedOperationException if the transport type is not supported
     */
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

    /**
     * Constructs server parameters used for STDIO transport including command, arguments,
     * and environment variables.
     *
     * @return a {@link ServerParameters} object containing startup details
     */
    @NonNull
    private ServerParameters createServerParameters() {
        return ServerParameters.builder(request.getRemoteServer().getCommand())
                .args(request.getRemoteServer().getRemoteServerArgs())
                .env(request.getRemoteServer().getRemoteServerEnvs())
                .build();
    }
}
