package io.github.makbn.mcp.mediator.api.plugin;

public interface McpMediatorResourceSpec<T> {
    String name();

    boolean authenticated();

    boolean privileged();

    T resource();
}
