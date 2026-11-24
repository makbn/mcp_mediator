package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMeta;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.modern.McpSchema;
import io.modelcontextprotocol.modern.client.McpClient;
import io.modelcontextprotocol.modern.client.McpSyncClient;
import io.modelcontextprotocol.modern.client.transport.HttpClientMcpTransport;
import io.modelcontextprotocol.modern.server.McpRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.context.McpModernSyncRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The SDK's "modern" server API (Spring AI 2.1's {@code Modern} providers) fed by the same annotated beans, called by the
 * SDK's modern client over HTTP. Both annotation families.
 */
@SpringBootTest(classes = ModernHttpTest.App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mcp.mediator.enabled=true", "mcp.mediator.transport=MODERN_HTTP"})
class ModernHttpTest {

    @EnableAutoConfiguration
    @Import({App.OwnBean.class, App.SpringAiBean.class})
    static class App {
        @Service
        static class OwnBean {
            @McpTool(name = "multiply", description = "multiplies", title = "Multiply")
            public int multiply(int a, int b) {
                return a * b;
            }

            @McpTool(name = "ctx", description = "context injection")
            public String ctx(McpRequestContext ctx, McpMeta meta, String x) {
                return ctx.method() + "|" + meta.get("trace") + "|" + x;
            }

            @McpTool(name = "needs_sampling", description = "x",
                    requiredCapabilities = io.github.makbn.mcp.mediator.api.McpClientCapability.SAMPLING)
            public String needsSampling() {
                return "sampled";
            }
        }

        @Service
        static class SpringAiBean {
            @org.springframework.ai.mcp.annotation.McpTool(name = "shout", description = "shouts")
            public String shout(String text, McpModernSyncRequestContext ctx) {
                return text.toUpperCase() + (ctx != null && ctx.context() != null ? "!" : "?");
            }

            @McpResource(uri = "mem://doc/{id}", name = "doc", mimeType = "text/markdown")
            public String doc(String id) {
                return "# doc " + id;
            }

            @McpResource(uri = "mem://static", name = "static", ttlMs = 5000,
                    cacheScope = McpSchema.CacheScope.PUBLIC,
                    annotations = @org.springframework.ai.mcp.annotation.McpResource.McpAnnotations(
                            audience = io.modelcontextprotocol.spec.McpSchema.Role.ASSISTANT, priority = 0.9))
            public String stat() {
                return "static";
            }

            @org.springframework.ai.mcp.annotation.McpTool(name = "ask_name", description = "elicits the user's name")
            public Object askName(McpModernSyncRequestContext ctx) {
                // inputResponses() rather than inputResponse(key, Class): Spring AI 2.1.0-RC2 was compiled against SDK
                // 2.2.0-M2 and its inputResponse(...) hits a NoSuchMethodError on the GA SDK (upstream version skew).
                Object answer = ctx.inputResponses() == null ? null : ctx.inputResponses().get("name");
                if (answer == null) {
                    return McpSchema.InputRequiredResult.builder().elicit("name", McpSchema.ElicitFormRequest.builder(
                            "What is your name?", Map.of("type", "object",
                                    "properties", Map.of("name", Map.of("type", "string")))).build()).build();
                }
                Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) answer).get("content");
                return "hello " + content.get("name");
            }

            @McpPrompt(name = "greet", description = "greets")
            public String greet(@McpArg(name = "name", required = true) String name) {
                return "Hello " + name;
            }

            @McpComplete(prompt = "greet")
            public List<String> complete(String value) {
                return List.of("alice", "alan", "bob").stream().filter(s -> s.startsWith(value)).toList();
            }
        }
    }

    @Value("${local.server.port}")
    int port;

    private McpSyncClient client(boolean sampling) {
        var transport = HttpClientMcpTransport.builder("http://localhost:" + port + "/mcp")
                .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper())).build();
        return McpClient.sync(transport).clientInfo(McpSchema.Implementation.builder("it", "1").build()).build();
    }

    private static String text(McpSchema.CallToolResult r) {
        return ((McpSchema.TextContent) r.content().get(0)).text();
    }

    @Test
    void toolsFromBothFamiliesAreListedAndCallable() {
        McpSyncClient c = client(false);
        List<McpSchema.Tool> tools = c.tools().list().tools();
        assertEquals(5, tools.size(), tools.toString());
        McpSchema.Tool multiply = tools.stream().filter(t -> t.name().equals("multiply")).findFirst().orElseThrow();
        assertEquals("Multiply", multiply.title());
        assertEquals("multiplies", multiply.description());

        assertEquals("42", text((McpSchema.CallToolResult) c.tool("multiply").arguments(Map.of("a", 6, "b", 7)).call()));
        assertTrue(text((McpSchema.CallToolResult) c.tool("shout").arguments(Map.of("text", "hi")).call()).contains("HI!"));
    }

    @Test
    void elicitationRoundTripThroughTheClientHandler() {
        var transport = HttpClientMcpTransport.builder("http://localhost:" + port + "/mcp")
                .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper())).build();
        McpSyncClient c = McpClient.sync(transport).clientInfo(McpSchema.Implementation.builder("it", "1").build())
                .formElicitation(req -> {
                    assertEquals("What is your name?", req.message());
                    return McpSchema.ElicitResult.accept(Map.of("name", "Ann"));
                }).build();
        McpSchema.CallToolResult r = (McpSchema.CallToolResult) c.tool("ask_name").call();
        assertTrue(text(r).contains("hello Ann"), r.toString());
    }

    @Test
    void resourceTtlCacheScopeAndAnnotationsAreApplied() {
        McpSyncClient c = client(false);
        McpSchema.Resource res = c.resources().list().resources().get(0);
        assertEquals(McpSchema.Role.ASSISTANT, res.annotations().audience().get(0));
        assertEquals(0.9, res.annotations().priority());
        McpSchema.ReadResourceResult read = (McpSchema.ReadResourceResult) c.resource("mem://static").read();
        assertEquals(5000L, read.ttlMs());
        assertEquals(McpSchema.CacheScope.PUBLIC, read.cacheScope());
    }

    @Test
    void requestContextAndMetaAreInjected() {
        McpSyncClient c = client(false);
        McpSchema.CallToolResult r = (McpSchema.CallToolResult) c.tool("ctx").arguments(Map.of("x", "v"))
                .meta(Map.of("trace", "t-1")).call();
        assertEquals("tools/call|t-1|v", text(r).replace("\"", ""));
    }

    @Test
    void requiredClientCapabilityIsEnforced() {
        McpSchema.CallToolResult r = (McpSchema.CallToolResult) client(false).tool("needs_sampling").call();
        assertTrue(Boolean.TRUE.equals(r.isError()), r.toString());
        assertTrue(text(r).contains("SAMPLING"), text(r));
    }

    @Test
    void resourcesTemplatesPromptsAndCompletionsWork() {
        McpSyncClient c = client(false);
        assertEquals("mem://static", c.resources().list().resources().get(0).uri());
        assertEquals("mem://doc/{id}", c.resourceTemplates().list().resourceTemplates().get(0).uriTemplate());
        var stat = c.resource("mem://static").read();
        assertEquals("static", ((McpSchema.TextResourceContents) ((McpSchema.ReadResourceResult) stat).contents().get(0)).text());
        var doc = c.resource("mem://doc/7").read();
        McpSchema.TextResourceContents dc = (McpSchema.TextResourceContents) ((McpSchema.ReadResourceResult) doc).contents().get(0);
        assertEquals("# doc 7", dc.text());
        assertEquals("text/markdown", dc.mimeType());

        assertEquals("greet", c.prompts().list().prompts().get(0).name());
        var prompt = (McpSchema.GetPromptResult) c.prompt("greet").arguments(Map.of("name", "Zed")).get();
        assertEquals("Hello Zed", ((McpSchema.TextContent) prompt.messages().get(0).content()).text());
        assertEquals(McpSchema.Role.ASSISTANT, prompt.messages().get(0).role());

        McpSchema.CompleteResult cr = c.completion(McpSchema.PromptReference.builder("greet").build(), "name", "al").complete();
        assertEquals(List.of("alice", "alan"), cr.completion().values());
    }

    /** Differential check against Spring AI 2.1's own Modern providers fed with the same bean. */
    @Test
    void definitionsMatchSpringAiModernProviders() {
        var bean = new App.SpringAiBean();
        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        McpSyncClient c = client(false);

        var refTools = new org.springframework.ai.mcp.annotation.provider.tool.SyncModernMcpToolProvider(List.of(bean)).getTools();
        assertEquals(2, refTools.size());
        McpSchema.Tool ref = refTools.stream().map(t -> t.definition()).filter(t -> t.name().equals("shout")).findFirst().orElseThrow();
        McpSchema.Tool ours = c.tools().list().tools().stream().filter(t -> t.name().equals(ref.name())).findFirst().orElseThrow();
        assertEquals(ref.description(), ours.description());
        assertEquals(new java.util.TreeSet<>(((Map<?, ?>) ref.inputSchema().get("properties")).keySet()),
                new java.util.TreeSet<>(((Map<?, ?>) ours.inputSchema().get("properties")).keySet()));
        assertEquals(String.valueOf(ref.inputSchema().get("required")), String.valueOf(ours.inputSchema().get("required")));

        var promptProvider = new org.springframework.ai.mcp.annotation.provider.prompt.SyncModernMcpPromptProvider(List.of(bean));
        promptProvider.setJsonMapper(jsonMapper);
        McpSchema.Prompt refPrompt = promptProvider.getPrompts().get(0).definition();
        McpSchema.Prompt ourPrompt = c.prompts().list().prompts().get(0);
        assertEquals(refPrompt.name(), ourPrompt.name());
        assertEquals(refPrompt.description(), ourPrompt.description());
        assertEquals(refPrompt.arguments().size(), ourPrompt.arguments().size());
        assertEquals(refPrompt.arguments().get(0).name(), ourPrompt.arguments().get(0).name());
        assertEquals(refPrompt.arguments().get(0).required(), ourPrompt.arguments().get(0).required());
    }
}
