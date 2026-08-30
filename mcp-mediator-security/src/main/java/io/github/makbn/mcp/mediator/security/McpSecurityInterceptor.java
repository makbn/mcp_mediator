package io.github.makbn.mcp.mediator.security;

import io.github.makbn.mcp.mediator.api.McpMediatorInterceptor;

/**
 * Generic interceptor that delegates to an McpTokenExtractor to find a token,
 * then places it into the McpSecurityContextHolder for the current thread.
 */
public class McpSecurityInterceptor implements McpMediatorInterceptor {

    private final McpTokenExtractor tokenExtractor;

    public McpSecurityInterceptor(McpTokenExtractor tokenExtractor) {
        this.tokenExtractor = tokenExtractor;
    }

    @Override
    public void intercept(Object request) throws Exception {
        McpSecurityContextHolder.clearContext();
        if (tokenExtractor != null) {
            McpAuthentication auth = tokenExtractor.extract(request);
            if (auth != null) {
                McpSecurityContext context = McpSecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth);
                McpSecurityContextHolder.setContext(context);
            }
        }
    }
}
