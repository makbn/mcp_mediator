package io.github.makbn.mcp.mediator.integration.config;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.core.AbstractMcpMediator;
import io.github.makbn.mcp.mediator.spring.SpringMcpMediatorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SpringMcpMediatorConfig.class)
public class TestConfig {

    @Bean
    public McpMediator mcpMediator() {
        return new AbstractMcpMediator() {
            @Override
            protected void doInitialize() {
                // No-op for tests
            }

            @Override
            protected void doStart() {
                // No-op for tests
            }

            @Override
            protected void doStop() {
                // No-op for tests
            }
        };
    }
} 