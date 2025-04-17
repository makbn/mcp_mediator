package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorProxyConfiguration;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
        delegateToRemoteServers();
    }

    private void delegateToRemoteServers() {

    }

    @NonNull
    private McpMediatorProxyConfiguration getProxyConfiguration() {
        return (McpMediatorProxyConfiguration) configuration;
    }

}
