package io.github.makbn.mcp.mediator.example;

import io.github.makbn.mcp.mediator.api.McpMediatorExceptionHandler;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorDefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomErrorHandlerExample {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorHandlerExample.class);

    public static void main(String[] args) {
        
        // 1. Create a custom Exception Handler
        McpMediatorExceptionHandler customHandler = new McpMediatorExceptionHandler() {
            @Override
            public String handleException(McpMediatorRequest<?> request, Throwable ex) {
                // Log the exception using slf4j with full context
                log.error("Failed to execute MCP tool! Request Details: {}", request, ex);

                // Optionally, obfuscate internal error details before sending to the client
                if (ex instanceof IllegalArgumentException) {
                    return "Invalid input provided: " + ex.getMessage();
                } else if (ex instanceof SecurityException) {
                    return "Authorization failed. Please provide a valid token.";
                }
                
                // Generic fallback message
                return "An internal server error occurred while processing the tool call.";
            }
        };

        // 2. Build configuration
        McpMediatorDefaultConfiguration config = McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName("safe-mcp-server")
                .exceptionHandler(customHandler)
                .build();

        DefaultMcpMediator mediator = new DefaultMcpMediator(config);
        
        log.info("Mediator initialized with custom SLF4J error handler mapping.");
    }
}
