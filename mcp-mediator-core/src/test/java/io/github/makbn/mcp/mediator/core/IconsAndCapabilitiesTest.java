package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.makbn.mcp.mediator.api.McpClientCapability;
import io.github.makbn.mcp.mediator.api.McpIcon;
import io.github.makbn.mcp.mediator.api.McpPrompt;
import io.github.makbn.mcp.mediator.api.McpTool;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** {@code icons} and {@code requiredCapabilities} for the own annotations and Spring AI 2.x's. */
class IconsAndCapabilitiesTest {

    public static class Own {
        @McpTool(name = "sample_tool", description = "needs sampling", requiredCapabilities = McpClientCapability.SAMPLING,
                icons = @McpIcon(src = "https://x/y.png", mimeType = "image/png", sizes = "48x48"))
        public String t() {
            return "ok";
        }

        @io.github.makbn.mcp.mediator.api.McpResource(uri = "mem://i", name = "i", icons = @McpIcon(src = "https://x/r.svg"),
                size = 5)
        public String r() {
            return "hello";
        }

        @McpPrompt(name = "pi", icons = @McpIcon(src = "https://x/p.png"), requiredCapabilities = McpClientCapability.ROOTS)
        public String p() {
            return "x";
        }
    }

    public static class SpringAi {
        @org.springframework.ai.mcp.annotation.McpTool(name = "sample_tool", description = "needs sampling",
                requiredCapabilities = org.springframework.ai.mcp.annotation.McpClientCapability.SAMPLING,
                icons = @org.springframework.ai.mcp.annotation.McpIcon(src = "https://x/y.png", mimeType = "image/png", sizes = "48x48"))
        public String t() {
            return "ok";
        }

        @org.springframework.ai.mcp.annotation.McpResource(uri = "mem://i", name = "i",
                icons = @org.springframework.ai.mcp.annotation.McpIcon(src = "https://x/r.svg"), size = 5)
        public String r() {
            return "hello";
        }

        @org.springframework.ai.mcp.annotation.McpPrompt(name = "pi",
                icons = @org.springframework.ai.mcp.annotation.McpIcon(src = "https://x/p.png"),
                requiredCapabilities = org.springframework.ai.mcp.annotation.McpClientCapability.ROOTS)
        public String p() {
            return "x";
        }
    }

    private static Object bean(String kind) {
        return kind.equals("own") ? new Own() : new SpringAi();
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void iconsAndSizeAreAdvertised(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(bean(kind)))) {
            JsonNode tool = h.request("tools/list", "{}").get("tools").get(0);
            assertEquals("https://x/y.png", tool.get("icons").get(0).get("src").asText());
            assertEquals("image/png", tool.get("icons").get(0).get("mimeType").asText());
            assertEquals("48x48", tool.get("icons").get(0).get("sizes").get(0).asText());
            JsonNode res = h.request("resources/list", "{}").get("resources").get(0);
            assertEquals("https://x/r.svg", res.get("icons").get(0).get("src").asText());
            assertEquals(5, res.get("size").asInt());
            assertEquals("https://x/p.png", h.request("prompts/list", "{}").get("prompts").get(0)
                    .get("icons").get(0).get("src").asText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void toolRequiringSamplingFailsWithoutThatClientCapability(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(bean(kind)))) {
            JsonNode r = h.request("tools/call", "{\"name\":\"sample_tool\",\"arguments\":{}}");
            assertTrue(r.path("isError").asBoolean(), r.toString());
            assertTrue(r.get("content").get(0).get("text").asText().contains("SAMPLING"), r.toString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void toolRequiringSamplingWorksWhenClientDeclaresIt(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(bean(kind)), "{\"sampling\":{}}")) {
            JsonNode r = h.request("tools/call", "{\"name\":\"sample_tool\",\"arguments\":{}}");
            assertFalse(r.path("isError").asBoolean(), r.toString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"own", "springai"})
    void promptRequiringRootsIsRejectedWithoutIt(String kind) throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(bean(kind)))) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> h.request("prompts/get", "{\"name\":\"pi\",\"arguments\":{}}"));
            assertTrue(e.getMessage().contains("ROOTS") || e.getMessage().contains("error"), e.getMessage());
        }
        try (var h = new StdioHarness(m -> m.registerBean(bean(kind)), "{\"roots\":{}}")) {
            assertNotNull(h.request("prompts/get", "{\"name\":\"pi\",\"arguments\":{}}"));
        }
    }

    public static class TeamMeta implements org.springframework.ai.mcp.annotation.context.MetaProvider {
        @Override
        public java.util.Map<String, Object> getMeta() {
            return java.util.Map.of("team", "platform");
        }
    }

    public static class WithMeta {
        @org.springframework.ai.mcp.annotation.McpTool(name = "m", description = "d", metaProvider = TeamMeta.class)
        public String t() {
            return "x";
        }

        @org.springframework.ai.mcp.annotation.McpResource(uri = "mem://m", name = "m", metaProvider = TeamMeta.class)
        public String r() {
            return "x";
        }

        @org.springframework.ai.mcp.annotation.McpPrompt(name = "mp", metaProvider = TeamMeta.class)
        public String p() {
            return "x";
        }
    }

    @org.junit.jupiter.api.Test
    void metaProviderValuesAreAdvertised() throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(new WithMeta()))) {
            assertEquals("platform", h.request("tools/list", "{}").get("tools").get(0).get("_meta").get("team").asText());
            assertEquals("platform", h.request("resources/list", "{}").get("resources").get(0).get("_meta").get("team").asText());
            assertEquals("platform", h.request("prompts/list", "{}").get("prompts").get(0).get("_meta").get("team").asText());
        }
    }
}
