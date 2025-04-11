package io.github.makbn.mcp.mediator.docker.spring.boot;

import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.docker.handler.DockerMcpRequestHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for Docker MCP handlers.
 */
@Configuration
@ConditionalOnProperty(name = "mcp.mediator.implementation-name", havingValue = "docker")
@EnableConfigurationProperties(DockerMcpMediatorProperties.class)
public class DockerMcpMediatorAutoConfiguration {

    @Bean
    public McpRequestHandler<?> dockerMcpRequestHandler() {
        return new DockerMcpRequestHandler();
    }
} 