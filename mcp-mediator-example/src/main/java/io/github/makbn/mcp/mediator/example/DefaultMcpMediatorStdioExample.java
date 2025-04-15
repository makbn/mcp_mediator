package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.McpMediatorConfiguration;
import io.github.makbn.mcp.mediator.docker.handler.DockerMcpRequestHandler;

public class DefaultMcpMediatorStdioExample {

    public static void main(String[] args) {
        DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfiguration.builder()
                .withDefaults()
                .serverName("my_example_mcp_server_stdio")
                .build());
        mediator.registerHandler(new DockerMcpRequestHandler());
        mediator.initialize();
    }
}
