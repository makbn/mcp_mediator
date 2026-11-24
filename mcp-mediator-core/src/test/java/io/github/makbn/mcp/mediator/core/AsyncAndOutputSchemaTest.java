package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.makbn.mcp.mediator.api.McpTool;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AsyncAndOutputSchemaTest {
    public record Person(String name, int age) {}

    public static class Fixture {
        @McpTool(name = "mono", description = "async single")
        public Mono<String> mono(String x) {
            return Mono.just("m:" + x);
        }

        @McpTool(name = "flux", description = "async many")
        public Flux<String> flux() {
            return Flux.just("a", "b");
        }

        @McpTool(name = "person", description = "structured", generateOutputSchema = true)
        public Person person(String name) {
            return new Person(name, 30);
        }

        @org.springaicommunity.mcp.annotation.McpTool(name = "person2", description = "structured spring ai",
                generateOutputSchema = true)
        public Person person2(String name) {
            return new Person(name, 31);
        }
    }

    @Test
    void reactiveMethodsAreResolved() throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(new Fixture()))) {
            assertEquals("m:1", h.request("tools/call", "{\"name\":\"mono\",\"arguments\":{\"x\":\"1\"}}")
                    .get("content").get(0).get("text").asText().replace("\"", ""));
            assertEquals("[\"a\",\"b\"]", h.request("tools/call", "{\"name\":\"flux\",\"arguments\":{}}")
                    .get("content").get(0).get("text").asText());
        }
    }

    @Test
    void outputSchemaAndStructuredContentForBothFamilies() throws IOException {
        try (var h = new StdioHarness(m -> m.registerBean(new Fixture()))) {
            for (String name : new String[]{"person", "person2"}) {
                JsonNode tool = null;
                for (JsonNode t : h.request("tools/list", "{}").get("tools")) if (t.get("name").asText().equals(name)) tool = t;
                assertNotNull(tool.get("outputSchema"), name);
                assertNotNull(tool.get("outputSchema").get("properties").get("age"));
                JsonNode r = h.request("tools/call", "{\"name\":\"" + name + "\",\"arguments\":{\"name\":\"Ann\"}}");
                assertFalse(r.path("isError").asBoolean(), r.toString());
                assertEquals("Ann", r.get("structuredContent").get("name").asText());
            }
            // tools without generateOutputSchema advertise none
            for (JsonNode t : h.request("tools/list", "{}").get("tools"))
                if (t.get("name").asText().equals("mono")) assertNull(t.get("outputSchema"));
        }
    }
}
