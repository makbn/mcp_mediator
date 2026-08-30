package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorDefaultConfiguration;
import io.github.makbn.mcp.mediator.security.McpAuthentication;
import io.github.makbn.mcp.mediator.security.McpTokenExtractor;
import io.github.makbn.mcp.mediator.spring.security.SpringSecurityMcpInterceptor;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpringSecurityExample {

    public static void main(String[] args) {
        // 1. Create a custom token extractor mapping to the generic McpAuthentication
        McpTokenExtractor extractor = request -> {
            if (request instanceof McpSchema.CallToolRequest callReq) {
                Object tokenObj = callReq.arguments().get("token");
                if (tokenObj instanceof String token) {
                    return new McpAuthentication() {
                        @Override
                        public boolean isAuthenticated() {
                            return true;
                        }

                        @Override
                        public Object getPrincipal() {
                            return token; // Simplistic token representation
                        }
                    };
                }
            }
            return null;
        };

        // 2. Use Spring Security's native DelegatingSecurityContextExecutorService
        ExecutorService delegatedExecutor = new DelegatingSecurityContextExecutorService(
                Executors.newCachedThreadPool()
        );

        // 3. Use the SpringSecurityMcpInterceptor which bridges McpAuthentication to Spring Authentication
        SpringSecurityMcpInterceptor springInterceptor = new SpringSecurityMcpInterceptor(extractor);

        // 4. Build configuration with interceptor and executor
        McpMediatorDefaultConfiguration config = McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName("spring-secure-mcp")
                .addInterceptor(springInterceptor)
                .executorService(delegatedExecutor)
                .build();

        DefaultMcpMediator mediator = new DefaultMcpMediator(config);
        
        System.out.println("Spring Security configured successfully.");
    }
}
