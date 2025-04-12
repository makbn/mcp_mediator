package io.github.makbn.mcp.mediator.spring.boot;

import io.github.makbn.mcp.mediator.api.McpMediator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for MCP Mediator.
 * Automatically configures the mediator based on application properties.
 */
@Configuration
@EnableConfigurationProperties(McpMediatorProperties.class)
public class McpMediatorAutoConfiguration {

   /* @Bean
    @ConditionalOnMissingBean
    public SpringMcpMediatorConfig mcpMediatorConfig(McpMediatorProperties properties) {
        SpringMcpMediatorConfig config = new SpringMcpMediatorConfig();
        config.setImplementationName(properties.getImplementationName());
        config.setProperties(properties.getProperties());
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public McpMediator mcpMediator(SpringMcpMediatorConfig config) {
        // This is a placeholder. Actual implementation will be provided by specific modules
        throw new UnsupportedOperationException("No MCP Mediator implementation found. Please add a specific implementation module.");
    }*/
} 