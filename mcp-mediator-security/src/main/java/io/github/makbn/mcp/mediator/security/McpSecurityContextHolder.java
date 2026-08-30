package io.github.makbn.mcp.mediator.security;

public class McpSecurityContextHolder {
    private static final ThreadLocal<McpSecurityContext> contextHolder = new ThreadLocal<>();

    public static void clearContext() {
        contextHolder.remove();
    }

    public static McpSecurityContext getContext() {
        McpSecurityContext ctx = contextHolder.get();
        if (ctx == null) {
            ctx = createEmptyContext();
            contextHolder.set(ctx);
        }
        return ctx;
    }

    public static void setContext(McpSecurityContext context) {
        if (context == null) {
            clearContext();
        } else {
            contextHolder.set(context);
        }
    }

    public static McpSecurityContext createEmptyContext() {
        return new DefaultMcpSecurityContext();
    }
}
