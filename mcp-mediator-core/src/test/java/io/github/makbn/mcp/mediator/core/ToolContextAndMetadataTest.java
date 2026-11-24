package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ToolContextAndMetadataTest {

    public static class Own {
        @McpTool(name = "ctx", title = "Context tool", description = "uses exchange",
                annotations = @McpTool.McpAnnotation(readOnlyHint = true, idempotentHint = true))
        public String ctx(McpSyncServerExchange exchange, String msg) {
            return "exchange=" + (exchange != null) + ",msg=" + msg;
        }
    }

    public static class SpringAi {
        @org.springaicommunity.mcp.annotation.McpTool(name = "ctx", title = "Context tool", description = "uses exchange",
                annotations = @org.springaicommunity.mcp.annotation.McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
        public String ctx(McpSyncServerExchange exchange, String msg) {
            return "exchange=" + (exchange != null) + ",msg=" + msg;
        }
    }

    public static class WithContext {
        @org.springaicommunity.mcp.annotation.McpTool(name = "rc", description = "request context")
        public String rc(org.springaicommunity.mcp.context.McpSyncRequestContext ctx, String msg) {
            return "ctx=" + (ctx != null && ctx.exchange() != null) + ",msg=" + msg;
        }
    }

    @org.junit.jupiter.api.Test
    void springAiRequestContextIsInjected() throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(new WithContext()))) {
            JsonNode props = h.request("tools/list", "{}").get("tools").get(0).get("inputSchema").get("properties");
            assertNull(props.get("ctx"));
            JsonNode r = h.request("tools/call", "{\"name\":\"rc\",\"arguments\":{\"msg\":\"x\"}}");
            assertFalse(r.path("isError").asBoolean(), r.toString());
            assertEquals("ctx=true,msg=x", r.get("content").get(0).get("text").asText().replace("\"", ""));
        }
    }

    public static class WithAsyncContext {
        @org.springaicommunity.mcp.annotation.McpTool(name = "arc", description = "async request context")
        public String arc(org.springaicommunity.mcp.context.McpAsyncRequestContext ctx,
                          io.modelcontextprotocol.server.McpAsyncServerExchange ex, String msg) {
            return "ctx=" + (ctx != null) + ",ex=" + (ex != null) + ",msg=" + msg;
        }
    }

    @org.junit.jupiter.api.Test
    void springAiAsyncRequestContextIsInjected() throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(new WithAsyncContext()))) {
            JsonNode props = h.request("tools/list", "{}").get("tools").get(0).get("inputSchema").get("properties");
            assertNull(props.get("ctx"));
            assertNull(props.get("ex"));
            JsonNode r = h.request("tools/call", "{\"name\":\"arc\",\"arguments\":{\"msg\":\"x\"}}");
            assertFalse(r.path("isError").asBoolean(), r.toString());
            assertEquals("ctx=true,ex=true,msg=x", r.get("content").get(0).get("text").asText().replace("\"", ""));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void exchangeIsInjectedHiddenFromSchemaAndMetadataIsAdvertised(String kind) throws IOException {
        Object bean = kind.equals("own") ? new Own() : new SpringAi();
        try (var h = new StdioHarness(m -> m.registerBean(bean))) {
            JsonNode tool = h.request("tools/list", "{}").get("tools").get(0);
            assertEquals("Context tool", tool.get("title").asText());
            assertTrue(tool.get("annotations").get("readOnlyHint").asBoolean());
            assertTrue(tool.get("annotations").get("idempotentHint").asBoolean());
            assertFalse(tool.get("annotations").get("destructiveHint").asBoolean());
            JsonNode props = tool.get("inputSchema").get("properties");
            assertNull(props.get("exchange"), "injected parameter must not be in the schema");
            assertNotNull(props.get("msg"));

            JsonNode result = h.request("tools/call", "{\"name\":\"ctx\",\"arguments\":{\"msg\":\"hi\"}}");
            assertFalse(result.path("isError").asBoolean(), result.toString());
            assertEquals("exchange=true,msg=hi", result.get("content").get(0).get("text").asText().replace("\"", ""));
        }
    }
}
