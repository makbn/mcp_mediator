package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.*;
import org.springframework.ai.mcp.annotation.provider.complete.SyncMcpCompleteProvider;
import org.springframework.ai.mcp.annotation.provider.prompt.SyncMcpPromptProvider;
import org.springframework.ai.mcp.annotation.provider.resource.SyncMcpResourceProvider;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Differential test: the same Spring AI-annotated bean is registered with Spring AI's own providers (the reference) and
 * with the mediator; advertised definitions and call results must match.
 */
class SpringAi2ReferenceParityTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public enum Color { RED, GREEN }

    public record Item(String name, int qty) {}

    public static class Bean {
        @McpTool(name = "sum", description = "sums")
        public int sum(@McpToolParam(description = "a", required = true) int a,
                       @McpToolParam(description = "b", required = true) int b) {
            return a + b;
        }

        @McpTool(name = "flags", description = "mixed types")
        public String flags(@McpToolParam(description = "flag") boolean flag,
                            @McpToolParam(description = "ratio") double ratio,
                            @McpToolParam(description = "color") Color color,
                            @McpToolParam(description = "tags", required = false) List<String> tags) {
            return flag + "/" + ratio + "/" + color + "/" + tags;
        }

        @McpTool(name = "item", description = "record in and out")
        public Item item(@McpToolParam(description = "item") Item item) {
            return new Item(item.name().toUpperCase(), item.qty() + 1);
        }

        @McpTool(name = "nothing", description = "void tool")
        public void nothing() {
        }

        @McpTool(name = "text", description = "plain string")
        public String text(@McpToolParam(description = "t") String t) {
            return "t=" + t;
        }

        @McpResource(uri = "mem://static", name = "static", description = "s", mimeType = "text/plain")
        public String stat() {
            return "static-body";
        }

        @McpResource(uri = "mem://annotated", name = "annotated",
                annotations = @McpResource.McpAnnotations(audience = io.modelcontextprotocol.spec.McpSchema.Role.ASSISTANT, priority = 0.2))
        public String annotated() {
            return "a";
        }

        @McpResource(uri = "mem://k/{key}/v/{ver}", name = "kv", description = "templ", mimeType = "application/json")
        public String kv(String key, String ver) {
            return "{\"k\":\"" + key + "\",\"v\":\"" + ver + "\"}";
        }

        @McpPrompt(name = "greet", description = "greeting prompt")
        public String greet(@McpArg(name = "name", description = "who", required = true) String name,
                            @McpArg(name = "formal", description = "formal?", required = false) String formal) {
            return "Hello " + name + (formal == null ? "" : " (" + formal + ")");
        }

        @McpComplete(prompt = "greet")
        public List<String> completeName(String value) {
            return List.of("alice", "bob", "alan").stream().filter(s -> s.startsWith(value)).toList();
        }
    }

    private static JsonNode tree(Object o) {
        return MAPPER.valueToTree(o);
    }

    private static Set<String> names(JsonNode arr) {
        Set<String> out = new TreeSet<>();
        if (arr != null) arr.forEach(n -> out.add(n.asText()));
        return out;
    }

    @Test
    void toolDefinitionsAndResultsMatchSpringAi() throws IOException {
        Bean bean = new Bean();
        List<McpServerFeatures.SyncToolSpecification> reference = new SyncMcpToolProvider(List.of(bean)).getToolSpecifications();
        assertEquals(5, reference.size());

        try (var h = new StdioHarness(m -> m.registerBean(bean))) {
            JsonNode ours = h.request("tools/list", "{}").get("tools");
            assertEquals(reference.size(), ours.size(), "tool count");
            for (var ref : reference) {
                McpSchema.Tool rt = ref.tool();
                JsonNode refSchema = tree(rt.inputSchema());
                JsonNode our = null;
                for (JsonNode t : ours) if (t.get("name").asText().equals(rt.name())) our = t;
                assertNotNull(our, "missing tool " + rt.name());
                assertEquals(rt.description(), our.get("description").asText(), rt.name() + " description");

                JsonNode ourSchema = our.get("inputSchema");
                assertEquals(names(refSchema.get("properties") == null ? null : tree(refSchema.get("properties").properties()
                                .stream().map(Map.Entry::getKey).toList())),
                        names(ourSchema.get("properties") == null ? null : tree(ourSchema.get("properties").properties()
                                .stream().map(Map.Entry::getKey).toList())), rt.name() + " property names");
                assertEquals(names(refSchema.get("required")), names(ourSchema.get("required")), rt.name() + " required");
                if (refSchema.get("properties") != null) {
                    refSchema.get("properties").properties().forEach(e -> {
                        JsonNode refProp = e.getValue();
                        JsonNode ourProp = ourSchema.get("properties").get(e.getKey());
                        assertEquals(refProp.path("type").asText(), ourProp.path("type").asText(),
                                rt.name() + "." + e.getKey() + " type");
                        assertEquals(refProp.path("description").asText(), ourProp.path("description").asText(),
                                rt.name() + "." + e.getKey() + " description");
                        assertEquals(names(refProp.get("enum")), names(ourProp.get("enum")),
                                rt.name() + "." + e.getKey() + " enum");
                        assertEquals(refProp.path("items").path("type").asText(), ourProp.path("items").path("type").asText(),
                                rt.name() + "." + e.getKey() + " items.type");
                    });
                }
            }

            // call results
            String[][] calls = {
                    {"sum", "{\"a\":2,\"b\":40}"},
                    {"flags", "{\"flag\":true,\"ratio\":1.5,\"color\":\"GREEN\",\"tags\":[\"x\",\"y\"]}"},
                    {"item", "{\"item\":{\"name\":\"ab\",\"qty\":1}}"},
                    {"text", "{\"t\":\"hi\"}"},
            };
            for (String[] c : calls) {
                McpSchema.CallToolRequest req = new McpSchema.CallToolRequest(c[0],
                        MAPPER.readValue(c[1], Map.class));
                var refSpec = reference.stream().filter(s -> s.tool().name().equals(c[0])).findFirst().orElseThrow();
                McpSchema.CallToolResult refResult = refSpec.callHandler().apply(null, req);
                JsonNode ourResult = h.request("tools/call", "{\"name\":\"" + c[0] + "\",\"arguments\":" + c[1] + "}");
                assertEquals(Boolean.TRUE.equals(refResult.isError()), ourResult.path("isError").asBoolean(), c[0] + " isError: " + ourResult);
                String refText = ((McpSchema.TextContent) refResult.content().get(0)).text();
                String ourText = ourResult.get("content").get(0).get("text").asText();
                assertEquals(norm(refText), norm(ourText), c[0] + " result text");
            }
        }
    }

    /** Both sides may or may not JSON-quote plain strings; compare the semantic value. */
    private static String norm(String s) {
        try {
            JsonNode n = MAPPER.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(s);
            return n.isTextual() ? n.asText() : n.toString();
        } catch (IOException e) {
            return s;
        }
    }

    @Test
    void resourcesPromptsAndCompletionsMatchSpringAi() throws IOException {
        Bean bean = new Bean();
        var resProvider = new SyncMcpResourceProvider(List.of(bean));
        var promptProvider = new SyncMcpPromptProvider(List.of(bean));
        var completeProvider = new SyncMcpCompleteProvider(List.of(bean));

        try (var h = new StdioHarness(m -> m.registerBean(bean))) {
            // resources
            var refRes = resProvider.getResourceSpecifications().stream().filter(r -> r.resource().uri().equals("mem://static")).findFirst().orElseThrow();
            JsonNode ourRes = null;
            for (JsonNode r : h.request("resources/list", "{}").get("resources")) {
                if (r.get("uri").asText().equals("mem://static")) ourRes = r;
            }
            assertEquals(refRes.resource().uri(), ourRes.get("uri").asText());
            assertEquals(refRes.resource().name(), ourRes.get("name").asText());
            assertEquals(refRes.resource().description(), ourRes.get("description").asText());
            assertEquals(refRes.resource().mimeType(), ourRes.get("mimeType").asText());
            assertNull(refRes.resource().annotations(), "default annotations are not advertised by Spring AI");
            assertNull(ourRes.get("annotations"), "nor by the mediator");
            // Spring AI's classic provider drops an explicit annotations() attribute; the mediator maps it per the MCP spec
            // (a superset of the reference), and both agree that defaults are not advertised.
            JsonNode ourAnnotated = null;
            for (JsonNode r : h.request("resources/list", "{}").get("resources")) {
                if (r.get("uri").asText().equals("mem://annotated")) ourAnnotated = r;
            }
            assertNotNull(ourAnnotated);
            assertEquals(0.2, ourAnnotated.get("annotations").get("priority").asDouble());
            assertEquals("assistant", ourAnnotated.get("annotations").get("audience").get(0).asText());
            var refRead = refRes.readHandler().apply(null, new McpSchema.ReadResourceRequest("mem://static"));
            JsonNode ourRead = h.request("resources/read", "{\"uri\":\"mem://static\"}").get("contents").get(0);
            assertEquals(((McpSchema.TextResourceContents) refRead.contents().get(0)).text(), ourRead.get("text").asText());

            // templates
            var refTpl = resProvider.getResourceTemplateSpecifications().get(0);
            JsonNode ourTpl = h.request("resources/templates/list", "{}").get("resourceTemplates").get(0);
            assertEquals(refTpl.resourceTemplate().uriTemplate(), ourTpl.get("uriTemplate").asText());
            assertEquals(refTpl.resourceTemplate().mimeType(), ourTpl.get("mimeType").asText());
            var refTplRead = refTpl.readHandler().apply(null, new McpSchema.ReadResourceRequest("mem://k/a/v/2"));
            JsonNode ourTplRead = h.request("resources/read", "{\"uri\":\"mem://k/a/v/2\"}").get("contents").get(0);
            assertEquals(((McpSchema.TextResourceContents) refTplRead.contents().get(0)).text(), ourTplRead.get("text").asText());
            assertEquals(refTplRead.contents().get(0).mimeType(), ourTplRead.get("mimeType").asText());

            // prompts
            var refPrompt = promptProvider.getPromptSpecifications().get(0);
            JsonNode ourPrompt = h.request("prompts/list", "{}").get("prompts").get(0);
            assertEquals(refPrompt.prompt().name(), ourPrompt.get("name").asText());
            assertEquals(refPrompt.prompt().description(), ourPrompt.get("description").asText());
            for (int i = 0; i < refPrompt.prompt().arguments().size(); i++) {
                var ra = refPrompt.prompt().arguments().get(i);
                JsonNode oa = ourPrompt.get("arguments").get(i);
                assertEquals(ra.name(), oa.get("name").asText());
                assertEquals(ra.description(), oa.get("description").asText());
                assertEquals(ra.required(), oa.get("required").asBoolean());
            }
            var refGet = refPrompt.promptHandler().apply(null,
                    new McpSchema.GetPromptRequest("greet", Map.of("name", "Zed", "formal", "yes")));
            JsonNode ourGet = h.request("prompts/get", "{\"name\":\"greet\",\"arguments\":{\"name\":\"Zed\",\"formal\":\"yes\"}}");
            assertEquals(((McpSchema.TextContent) refGet.messages().get(0).content()).text(),
                    ourGet.get("messages").get(0).get("content").get("text").asText());
            assertEquals(refGet.messages().get(0).role().name().toLowerCase(),
                    ourGet.get("messages").get(0).get("role").asText());

            // completions
            var refComplete = completeProvider.getCompleteSpecifications().get(0);
            var req = new McpSchema.CompleteRequest(refComplete.referenceKey(),
                    new McpSchema.CompleteRequest.CompleteArgument("name", "al"));
            List<String> refValues = refComplete.completionHandler().apply(null, req).completion().values();
            JsonNode ourComplete = h.request("completion/complete",
                    "{\"ref\":{\"type\":\"ref/prompt\",\"name\":\"greet\"},\"argument\":{\"name\":\"name\",\"value\":\"al\"}}")
                    .get("completion").get("values");
            assertEquals(new TreeSet<>(refValues), names(ourComplete));
        }
    }
}
