package io.github.makbn.mcp.mediator.spring.security;

import io.github.makbn.mcp.mediator.api.McpMediatorInterceptor;
import io.github.makbn.mcp.mediator.security.McpAuthentication;
import io.github.makbn.mcp.mediator.security.McpTokenExtractor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * An interceptor that bridges generic MCP token extraction with Spring Security.
 */
public class SpringSecurityMcpInterceptor implements McpMediatorInterceptor {

    private final McpTokenExtractor tokenExtractor;
    private final AuthenticationManager authenticationManager;

    public SpringSecurityMcpInterceptor(McpTokenExtractor tokenExtractor, AuthenticationManager authenticationManager) {
        this.tokenExtractor = tokenExtractor;
        this.authenticationManager = authenticationManager;
    }

    public SpringSecurityMcpInterceptor(McpTokenExtractor tokenExtractor) {
        this(tokenExtractor, null);
    }

    @Override
    public void intercept(Object request) throws Exception {
        SecurityContextHolder.clearContext();
        if (tokenExtractor != null) {
            McpAuthentication mcpAuth = tokenExtractor.extract(request);
            if (mcpAuth != null) {
                Authentication springAuth = convert(mcpAuth);
                if (authenticationManager != null) {
                    springAuth = authenticationManager.authenticate(springAuth);
                }
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(springAuth);
                SecurityContextHolder.setContext(context);
            }
        }
    }

    /**
     * Converts a generic McpAuthentication to a Spring Authentication.
     */
    protected Authentication convert(McpAuthentication mcpAuth) {
        // By default, create an unauthenticated token with the principal
        return new UsernamePasswordAuthenticationToken(mcpAuth.getPrincipal(), null, java.util.List.of());
    }
}
