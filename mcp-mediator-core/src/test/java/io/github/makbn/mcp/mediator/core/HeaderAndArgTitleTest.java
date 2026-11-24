package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.core.internal.McpMethodSchemaGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** {@code @McpToolParam(header = true)} (x-mcp-header) and {@code @McpArg(title)} for both annotation families. */
class HeaderAndArgTitleTest {
    public static class Own {
        @io.github.makbn.mcp.mediator.api.McpTool(name = "t", description = "d")
        public String t(@io.github.makbn.mcp.mediator.api.McpToolParam(header = true) String region, int n) {
            return region + n;
        }

        @io.github.makbn.mcp.mediator.api.McpPrompt(name = "p")
        public String p(@io.github.makbn.mcp.mediator.api.McpArg(title = "Who") String who) {
            return who;
        }

        @io.github.makbn.mcp.mediator.api.McpTool(name = "bad")
        public String bad(@io.github.makbn.mcp.mediator.api.McpToolParam(header = true) java.util.List<String> xs) {
            return "";
        }
    }

    public static class SpringAi {
        @org.springframework.ai.mcp.annotation.McpTool(name = "t", description = "d")
        public String t(@org.springframework.ai.mcp.annotation.McpToolParam(header = true) String region, int n) {
            return region + n;
        }

        @org.springframework.ai.mcp.annotation.McpPrompt(name = "p")
        public String p(@org.springframework.ai.mcp.annotation.McpArg(name = "who", title = "Who") String who) {
            return who;
        }

        @org.springframework.ai.mcp.annotation.McpTool(name = "bad")
        public String bad(@org.springframework.ai.mcp.annotation.McpToolParam(header = true) java.util.List<String> xs) {
            return "";
        }
    }

    @Test
    void headerParameterCarriesXMcpHeaderKeyword() throws Exception {
        for (Class<?> type : new Class<?>[]{Own.class, SpringAi.class}) {
            JsonNode schema = new ObjectMapper().readTree(McpMethodSchemaGenerator.of(new ObjectMapper())
                    .generateSchemaForMethod(type.getMethod("t", String.class, int.class)));
            assertEquals("region", schema.get("properties").get("region").get("x-mcp-header").asText(), type.getName());
            assertNull(schema.get("properties").get("n").get("x-mcp-header"));
        }
    }

    @Test
    void headerOnNonScalarParameterIsRejected() {
        for (Class<?> type : new Class<?>[]{Own.class, SpringAi.class}) {
            assertThrows(McpMediatorException.class, () -> McpMethodSchemaGenerator.of(new ObjectMapper())
                    .generateSchemaForMethod(type.getMethod("bad", java.util.List.class)), type.getName());
        }
    }

    @Test
    void promptArgumentTitleIsAdvertised() throws IOException {
        for (Object bean : new Object[]{new Own(), new SpringAi()}) {
            try (var h = new StdioHarness(m -> {
                // register only the prompt-bearing methods: the 'bad' tool would fail schema generation by design
                m.registerBean(bean instanceof Own ? new OwnPromptOnly() : new SpringAiPromptOnly());
            })) {
                assertEquals("Who", h.request("prompts/list", "{}").get("prompts").get(0).get("arguments").get(0).get("title").asText());
            }
        }
    }

    public static class OwnPromptOnly {
        @io.github.makbn.mcp.mediator.api.McpPrompt(name = "p")
        public String p(@io.github.makbn.mcp.mediator.api.McpArg(title = "Who") String who) {
            return who;
        }
    }

    public static class SpringAiPromptOnly {
        @org.springframework.ai.mcp.annotation.McpPrompt(name = "p")
        public String p(@org.springframework.ai.mcp.annotation.McpArg(name = "who", title = "Who") String who) {
            return who;
        }
    }
}
