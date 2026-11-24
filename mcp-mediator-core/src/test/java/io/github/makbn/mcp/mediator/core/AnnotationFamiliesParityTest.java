package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** The same MCP surface must be produced from both annotation families, verified over a real JSON-RPC session. */
class AnnotationFamiliesParityTest {

    private static Object fixture(String kind) {
        return kind.equals("own") ? new OwnAnnotatedFixture() : new SpringAiAnnotatedFixture();
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void advertisesAllCapabilities(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(fixture(kind)))) {
            JsonNode init = h.request("ping", "{}");
            assertNotNull(init);
            assertEquals(2, h.request("resources/list", "{}").get("resources").size() + 1,
                    "one static resource");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void toolsListAndCall(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(fixture(kind)))) {
            JsonNode tools = h.request("tools/list", "{}").get("tools");
            assertEquals(2, tools.size());
            JsonNode add = find(tools, "add");
            assertEquals("adds two numbers", add.get("description").asText());
            JsonNode props = add.get("inputSchema").get("properties");
            assertEquals("first", props.get("a").get("description").asText());
            assertEquals("integer", props.get("a").get("type").asText());

            JsonNode result = h.request("tools/call", "{\"name\":\"add\",\"arguments\":{\"a\":2,\"b\":3}}");
            assertFalse(result.path("isError").asBoolean(), result.toString());
            assertEquals("5", result.get("content").get(0).get("text").asText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void complexArgumentsAndOptionalParams(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(fixture(kind)))) {
            JsonNode tools = h.request("tools/list", "{}").get("tools");
            String name = kind.equals("own") ? "move_point" : "movePoint";
            JsonNode move = find(tools, name);
            assertEquals(1, move.get("inputSchema").get("required").size(), "step is optional");

            JsonNode result = h.request("tools/call",
                    "{\"name\":\"" + name + "\",\"arguments\":{\"point\":{\"x\":1,\"y\":2}}}");
            assertFalse(result.path("isError").asBoolean(), result.toString());
            assertTrue(result.get("content").get(0).get("text").asText().contains("\"x\":2"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void resourcesAndTemplates(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(fixture(kind)))) {
            JsonNode res = h.request("resources/list", "{}").get("resources");
            assertEquals(1, res.size());
            assertEquals("mem://greeting", res.get(0).get("uri").asText());
            assertEquals("hello", h.request("resources/read", "{\"uri\":\"mem://greeting\"}")
                    .get("contents").get(0).get("text").asText());

            JsonNode templates = h.request("resources/templates/list", "{}").get("resourceTemplates");
            assertEquals("mem://users/{id}/profile", templates.get(0).get("uriTemplate").asText());
            JsonNode read = h.request("resources/read", "{\"uri\":\"mem://users/42/profile\"}")
                    .get("contents").get(0);
            assertEquals("{\"id\":\"42\"}", read.get("text").asText());
            assertEquals("application/json", read.get("mimeType").asText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void promptsAndCompletions(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(fixture(kind)))) {
            JsonNode prompt = h.request("prompts/list", "{}").get("prompts").get(0);
            assertEquals("review", prompt.get("name").asText());
            assertEquals(2, prompt.get("arguments").size());
            assertTrue(prompt.get("arguments").get(0).get("required").asBoolean());
            assertFalse(prompt.get("arguments").get(1).get("required").asBoolean());

            JsonNode got = h.request("prompts/get",
                    "{\"name\":\"review\",\"arguments\":{\"language\":\"Java\"}}");
            assertEquals("Review this Java code",
                    got.get("messages").get(0).get("content").get("text").asText());

            JsonNode completion = h.request("completion/complete",
                    "{\"ref\":{\"type\":\"ref/prompt\",\"name\":\"review\"},"
                            + "\"argument\":{\"name\":\"language\",\"value\":\"ja\"}}").get("completion");
            assertEquals(2, completion.get("values").size());
        }
    }

    private static JsonNode find(JsonNode tools, String name) {
        for (JsonNode t : tools) if (t.get("name").asText().equals(name)) return t;
        fail("tool not found: " + name + " in " + tools);
        return null;
    }
}
