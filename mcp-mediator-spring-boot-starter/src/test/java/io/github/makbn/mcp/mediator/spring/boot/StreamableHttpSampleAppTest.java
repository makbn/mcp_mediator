package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpPrompt;
import io.github.makbn.mcp.mediator.api.McpTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sample Spring Boot application: ordinary beans, one with the mediator's own annotations and one with Spring AI's,
 * served over streamable HTTP and called with a real HTTP client against an embedded server.
 */
@SpringBootTest(classes = StreamableHttpSampleAppTest.SampleApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mcp.mediator.enabled=true", "mcp.mediator.transport=STREAMABLE_HTTP"})
class StreamableHttpSampleAppTest {

    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.context.annotation.Import({SampleApp.Calculator.class, SampleApp.Prompts.class})
    static class SampleApp {
        @Service
        static class Calculator {
            @McpTool(name = "multiply", description = "multiplies")
            public int multiply(int a, int b) {
                return a * b;
            }
        }

        @Service
        static class Prompts {
            @org.springaicommunity.mcp.annotation.McpPrompt(name = "hello", description = "greeting")
            public String hello(@org.springaicommunity.mcp.annotation.McpArg(name = "who") String who) {
                return "Hello " + who;
            }
        }
    }

    @Value("${local.server.port}")
    int port;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String body, String sessionId) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (sessionId != null) b.header("Mcp-Session-Id", sessionId);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Responses are JSON or a single SSE "data:" frame depending on the transport. */
    private JsonNode json(String body) throws Exception {
        for (String line : body.split("\n")) {
            String l = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
            if (l.startsWith("{")) return mapper.readTree(l);
        }
        throw new IllegalStateException("no JSON in: " + body);
    }

    @Test
    void serverIsReachableOverStreamableHttpWithBothAnnotationFamilies() throws Exception {
        HttpResponse<String> init = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
                + "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}", null);
        assertEquals(200, init.statusCode(), init.body());
        String session = init.headers().firstValue("Mcp-Session-Id").orElse(null);
        assertNotNull(json(init.body()).get("result").get("capabilities").get("tools"));
        post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", session);

        JsonNode call = json(post("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":"
                + "{\"name\":\"multiply\",\"arguments\":{\"a\":6,\"b\":7}}}", session).body());
        assertEquals("42", call.get("result").get("content").get(0).get("text").asText());

        JsonNode prompt = json(post("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"prompts/get\",\"params\":"
                + "{\"name\":\"hello\",\"arguments\":{\"who\":\"MCP\"}}}", session).body());
        assertEquals("Hello MCP", prompt.get("result").get("messages").get(0).get("content").get("text").asText());
    }
}
