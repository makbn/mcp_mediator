package io.github.makbn.mcp.mediator.core;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * The MCP exchange and request of the tool call being executed on the current thread. Tool methods can simply declare
 * a {@link McpSyncServerExchange} (logging, progress, sampling, elicitation, roots) or
 * {@link McpSchema.CallToolRequest} parameter; the mediator injects it and keeps it out of the JSON schema.
 */
public final class McpCallContext {
    /** Spring AI 1.x: org.springaicommunity.mcp.{annotation,context}; 2.x: org.springframework.ai.mcp.annotation[.context]. */
    private static boolean isSpringAiType(Class<?> t, String simpleName) {
        String n = t.getName();
        return n.equals("org.springaicommunity.mcp.annotation." + simpleName)
                || n.equals("org.springaicommunity.mcp.context." + simpleName)
                || n.equals("org.springframework.ai.mcp.annotation." + simpleName)
                || n.equals("org.springframework.ai.mcp.annotation.context." + simpleName);
    }

    private static final ThreadLocal<McpCallContext> CURRENT = new ThreadLocal<>();

    private final McpSyncServerExchange exchange;
    private final McpSchema.CallToolRequest request;
    private io.modelcontextprotocol.modern.server.McpRequestContext modernContext;
    private java.util.Map<String, Object> modernInputResponses;
    private String modernRequestState;
    private io.modelcontextprotocol.json.McpJsonMapper modernJson;

    private McpCallContext(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        this.exchange = exchange;
        this.request = request;
    }

    public static McpCallContext get() {
        return CURRENT.get();
    }

    static void set(McpCallContext context) {
        if (context == null) CURRENT.remove();
        else CURRENT.set(context);
    }

    /** Context of a call served by the "modern" MCP server. */
    static McpCallContext modern(io.modelcontextprotocol.modern.server.McpRequestContext ctx,
                                 java.util.Map<String, Object> inputResponses, String requestState,
                                 io.modelcontextprotocol.json.McpJsonMapper json) {
        McpCallContext c = new McpCallContext(null, null);
        c.modernContext = ctx;
        c.modernInputResponses = inputResponses;
        c.modernRequestState = requestState;
        c.modernJson = json;
        return c;
    }

    static McpCallContext of(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        return new McpCallContext(exchange, request);
    }

    public McpSyncServerExchange exchange() {
        return exchange;
    }

    public McpSchema.CallToolRequest request() {
        return request;
    }

    /** @return true for parameter types the mediator injects instead of reading them from the tool arguments. */
    public static boolean isInjectable(java.lang.reflect.Parameter parameter) {
        Class<?> t = parameter.getType();
        return t == McpSyncServerExchange.class || t == McpSchema.CallToolRequest.class
                || isSpringAiType(t, "McpSyncRequestContext") || isSpringAiType(t, "McpAsyncRequestContext")
                || t == io.modelcontextprotocol.server.McpAsyncServerExchange.class
                || t == io.modelcontextprotocol.modern.server.McpRequestContext.class
                || isSpringAiType(t, "McpModernSyncRequestContext")
                || t == io.github.makbn.mcp.mediator.api.McpMeta.class || isSpringAiType(t, "McpMeta")
                || io.github.makbn.mcp.mediator.core.util.McpAnnotations.find(parameter, "McpProgressToken").isPresent();
    }

    /** Value to inject for an {@link #isInjectable(java.lang.reflect.Parameter)} parameter, or null outside a call. */
    public static Object injectedValue(java.lang.reflect.Parameter parameter) {
        McpCallContext c = CURRENT.get();
        if (c == null) return null;
        if (parameter.getType() == McpSyncServerExchange.class) return c.exchange;
        if (parameter.getType() == McpSchema.CallToolRequest.class) return c.request;
        if (c.modernContext != null) {
            return modernValue(parameter, c);
        }
        if (parameter.getType() == io.github.makbn.mcp.mediator.api.McpMeta.class) {
            return new io.github.makbn.mcp.mediator.api.McpMeta(c.request == null ? null : c.request.meta());
        }
        if (isSpringAiType(parameter.getType(), "McpMeta")) {
            try {
                return parameter.getType().getConstructor(java.util.Map.class)
                        .newInstance(c.request == null ? java.util.Map.of() : c.request.meta());
            } catch (ReflectiveOperationException e) {
                throw new io.github.makbn.mcp.mediator.api.McpMediatorException("Can't create McpMeta", e);
            }
        }
        if (parameter.getType() == io.modelcontextprotocol.server.McpAsyncServerExchange.class) {
            return asyncExchange(c);
        }
        if (isSpringAiType(parameter.getType(), "McpAsyncRequestContext")) {
            return springAiAsyncContext(parameter.getType(), c);
        }
        if (isSpringAiType(parameter.getType(), "McpSyncRequestContext")) {
            return springAiContext(parameter.getType(), c);
        }
        Object token = c.request == null ? null : c.request.progressToken();
        return token == null ? null : token.toString();
    }

    /** Builds Spring AI's McpSyncRequestContext reflectively so Spring AI stays an optional dependency. */
    private static Object springAiContext(Class<?> type, McpCallContext c) {
        try {
            Class<?> impl = Class.forName(type.getPackageName() + ".DefaultMcpSyncRequestContext", true,
                    type.getClassLoader());
            Object builder = impl.getMethod("builder").invoke(null);
            builder.getClass().getMethod("request", McpSchema.Request.class).invoke(builder, c.request);
            builder.getClass().getMethod("exchange", McpSyncServerExchange.class).invoke(builder, c.exchange);
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (ReflectiveOperationException e) {
            throw new io.github.makbn.mcp.mediator.api.McpMediatorException("Can't create McpSyncRequestContext", e);
        }
    }

    /** The sync exchange wraps an async one; the SDK offers no accessor, so read its field. */
    private static io.modelcontextprotocol.server.McpAsyncServerExchange asyncExchange(McpCallContext c) {
        if (c.exchange == null) {
            return null; // stateless transport: there is no session exchange
        }
        try {
            java.lang.reflect.Field f = McpSyncServerExchange.class.getDeclaredField("exchange");
            f.setAccessible(true);
            return (io.modelcontextprotocol.server.McpAsyncServerExchange) f.get(c.exchange);
        } catch (ReflectiveOperationException e) {
            throw new io.github.makbn.mcp.mediator.api.McpMediatorException("Can't access the async exchange", e);
        }
    }

    private static Object springAiAsyncContext(Class<?> type, McpCallContext c) {
        try {
            Class<?> impl = Class.forName(type.getPackageName() + ".DefaultMcpAsyncRequestContext", true,
                    type.getClassLoader());
            Object builder = impl.getMethod("builder").invoke(null);
            builder.getClass().getMethod("request", McpSchema.Request.class).invoke(builder, c.request);
            builder.getClass().getMethod("exchange", io.modelcontextprotocol.server.McpAsyncServerExchange.class)
                    .invoke(builder, asyncExchange(c));
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (ReflectiveOperationException e) {
            throw new io.github.makbn.mcp.mediator.api.McpMediatorException("Can't create McpAsyncRequestContext", e);
        }
    }

    private static Object modernValue(java.lang.reflect.Parameter parameter, McpCallContext c) {
        Class<?> t = parameter.getType();
        if (t == io.modelcontextprotocol.modern.server.McpRequestContext.class) {
            return c.modernContext;
        }
        if (t == io.github.makbn.mcp.mediator.api.McpMeta.class) {
            return new io.github.makbn.mcp.mediator.api.McpMeta(c.modernContext.meta());
        }
        if (isSpringAiType(t, "McpMeta")) {
            try {
                return t.getConstructor(java.util.Map.class).newInstance(c.modernContext.meta());
            } catch (ReflectiveOperationException e) {
                throw new io.github.makbn.mcp.mediator.api.McpMediatorException("Can't create McpMeta", e);
            }
        }
        if (isSpringAiType(t, "McpModernSyncRequestContext")) {
            try {
                Class<?> impl = Class.forName(t.getPackageName() + ".DefaultMcpModernSyncRequestContext", true, t.getClassLoader());
                Object b = impl.getMethod("builder").invoke(null);
                Class<?> bc = b.getClass();
                bc.getMethod("context", io.modelcontextprotocol.modern.server.McpRequestContext.class).invoke(b, c.modernContext);
                bc.getMethod("inputResponses", java.util.Map.class).invoke(b, c.modernInputResponses == null ? java.util.Map.of() : c.modernInputResponses);
                bc.getMethod("requestState", String.class).invoke(b, c.modernRequestState);
                bc.getMethod("jsonMapper", io.modelcontextprotocol.json.McpJsonMapper.class).invoke(b, c.modernJson);
                return bc.getMethod("build").invoke(b);
            } catch (ReflectiveOperationException e) {
                throw new io.github.makbn.mcp.mediator.api.McpMediatorException("Can't create McpModernSyncRequestContext", e);
            }
        }
        Object token = c.modernContext.progressToken();
        return token == null ? null : token.toString(); // @McpProgressToken
    }
}
