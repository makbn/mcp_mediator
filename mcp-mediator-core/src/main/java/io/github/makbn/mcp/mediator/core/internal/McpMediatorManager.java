package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.api.plugin.McpMediatorResourceSpec;

import java.util.Collection;
import java.util.ServiceLoader;

public class McpMediatorManager {

    public Collection<McpMediatorResourceSpec> getMediatorResources() {
        ServiceLoader<McpMediatorResourceSpec> resources = ServiceLoader.load(McpMediatorResourceSpec.class);
        return resources.stream().map(ServiceLoader.Provider::get).toList();
    }
}
