package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.docker.handler.DockerMcpRequestHandler;

public class DefaultMcpMediatorStdioExample {

    public static final String MY_EXAMPLE_MCP_SERVER_STDIO = "my_example_mcp_server_stdio";

    public static void main(String[] args) {
        DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName(MY_EXAMPLE_MCP_SERVER_STDIO)
                .serverVersion("1.0.0.0")
                .build());
        mediator.registerHandler(new DockerMcpRequestHandler());
        mediator.initialize();
    }
}
