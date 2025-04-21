package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorProxyConfiguration;
import io.github.makbn.mcp.mediator.core.internal.McpLifecycleInitializationRequest;
import io.github.makbn.mcp.mediator.core.internal.McpMediatorRemoteMcpServer;
import io.github.makbn.mcp.mediator.core.internal.McpRemoteServerToolAdapter;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class ProxyMcpMediator extends DefaultMcpMediator {

    public ProxyMcpMediator() {
        this(McpMediatorConfigurationBuilder.builder().creatProxy().build());
    }

    public ProxyMcpMediator(@NonNull McpMediatorProxyConfiguration configuration) {
        super(configuration);

    }

    @Override
    protected void delegate() {
        super.delegate();
        delegateToRemoteServers(getProxyConfiguration().getRemoteMcpServerConfigurations());
    }

    private void delegateToRemoteServers(
            @NonNull List<McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration> remoteServers) {
        log.info("Starting remote MCP servers");
        remoteServers.forEach(server -> {
            log.debug("Gathering information for {}", server);
            McpMediatorRemoteMcpServer remoteMcpServer = getProvidedToolsByMcpServer(server);
            log.debug("Remote Server responded properly {}", remoteMcpServer);
            remoteMcpServer.getToolAdapters().forEach(providedTool ->
                    mcpSyncServer.addTool(createMcpToolSpecification(providedTool,
                            invocationParameters -> remoteMcpServer.handleRemoteRequest(providedTool, invocationParameters))
                    ));
        });
        mcpSyncServer.notifyToolsListChanged();
        log.debug("all remote MCP servers started successfully {}", mcpSyncServer);
    }

    @NonNull
    private McpMediatorRemoteMcpServer getProvidedToolsByMcpServer(
            @NonNull McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration server) {

        McpLifecycleInitializationRequest request = McpLifecycleInitializationRequest.builder()
                .id(1)
                .params(McpLifecycleInitializationRequest.McpLifecycleInitializationRequestParam.builder()
                        .clientName("mcp_mediator_proxy_server_for" + server)
                        .clientVersion("1.0.0.0")
                        .build())
                .remoteServer(server)
                .build();

        return McpRemoteServerToolAdapter.of(request)
                .getRemoteServer();
    }

    @NonNull
    private McpMediatorProxyConfiguration getProxyConfiguration() {
        return (McpMediatorProxyConfiguration) configuration;
    }

}
