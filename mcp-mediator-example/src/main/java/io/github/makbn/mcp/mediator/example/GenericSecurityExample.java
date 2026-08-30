package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorDefaultConfiguration;
import io.github.makbn.mcp.mediator.security.DelegatingMcpSecurityContextExecutorService;
import io.github.makbn.mcp.mediator.security.McpAuthentication;
import io.github.makbn.mcp.mediator.security.McpSecurityInterceptor;
import io.github.makbn.mcp.mediator.security.McpTokenExtractor;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GenericSecurityExample {

    public static void main(String[] args) {
        // 1. Create a custom token extractor
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
                            return token; // simplistic token auth
                        }
                    };
                }
            }
            return null;
        };

        // 2. Wrap executor to propagate security context
        ExecutorService delegatedExecutor = new DelegatingMcpSecurityContextExecutorService(
                Executors.newCachedThreadPool()
        );

        // 3. Build configuration with interceptor and executor
        McpMediatorDefaultConfiguration config = McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName("generic-secure-mcp")
                .addInterceptor(new McpSecurityInterceptor(extractor))
                .executorService(delegatedExecutor)
                .build();

        DefaultMcpMediator mediator = new DefaultMcpMediator(config);
        
        // Start your mediator (e.g., blocking stdio loop)
        // mediator.initialize();
        // mediator.start();
        System.out.println("Generic Security configured successfully.");
    }
}
