package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.McpMediatorConfiguration;
import io.github.makbn.mcp.mediator.docker.handler.DockerMcpRequestHandler;

public class DefaultMcpMediatorStdioExample {

    public static final String MY_EXAMPLE_MCP_SERVER_STDIO = "my_example_mcp_server_stdio";

    public static void main(String[] args) {
        DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfiguration.builder()
                .withDefaults()
                .serverName(MY_EXAMPLE_MCP_SERVER_STDIO)
                .build());
        mediator.registerHandler(new DockerMcpRequestHandler());
        mediator.initialize();
    }
}
