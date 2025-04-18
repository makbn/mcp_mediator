package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorProxyConfiguration;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
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
        remoteServers.forEach(server ->
                getProvidedToolsByMcpServer(server).forEach(providedTool ->
            mcpSyncServer.addTool(createMcpToolSpecification(providedTool, this::handleRemoteRequest))
        ));

    }

    @NonNull
    private List<McpSchema.Content> handleRemoteRequest(@NonNull Map<String, Object> clientPassedArgs) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @NonNull
    private List<McpRequestAdapter> getProvidedToolsByMcpServer(@NonNull McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration server) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @NonNull
    private McpMediatorProxyConfiguration getProxyConfiguration() {
        return (McpMediatorProxyConfiguration) configuration;
    }

}
