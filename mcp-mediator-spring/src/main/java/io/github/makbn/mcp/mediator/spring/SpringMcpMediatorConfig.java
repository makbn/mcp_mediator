package io.github.makbn.mcp.mediator.spring;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.core.GenericMcpMediator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Spring configuration for MCP Mediator.
 * Automatically registers request handlers from the Spring context.
 */
@Configuration
public class SpringMcpMediatorConfig {

    @Autowired
    private McpMediator mediator;

    @Autowired(required = false)
    private List<McpRequestHandler<?>> requestHandlers;

    @Bean
    public McpMediator mcpMediator() {
        return new GenericMcpMediator();
    }

    @PostConstruct
    public void registerHandlers() {
        if (requestHandlers != null) {
            for (McpRequestHandler<?> handler : requestHandlers) {
                mediator.registerHandler(handler);
            }
        }
    }
} 