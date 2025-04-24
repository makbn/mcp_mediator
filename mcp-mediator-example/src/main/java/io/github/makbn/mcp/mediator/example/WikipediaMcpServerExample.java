package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.docker.handler.WikipediaQueryRequestHandler;
import io.github.makbn.mcp.mediator.query.handler.WikipediaSummaryRequestHandler;
import lombok.NonNull;

public class WikipediaMcpServerExample {
    private static final @NonNull String WIKIPEDIA_MCP_SERVER = "wikipedia_mcp_server_stdio";

    public static void main(String[] args) {
        DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName(WIKIPEDIA_MCP_SERVER)
                .serverVersion("1.0.0.0")
                .build());
        mediator.registerHandler(new WikipediaSummaryRequestHandler());
        mediator.registerHandler(new WikipediaQueryRequestHandler());
        mediator.initialize();
    }
}
