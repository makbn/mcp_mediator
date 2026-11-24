package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpPrompt;
import io.github.makbn.mcp.mediator.api.McpResource;
import io.github.makbn.mcp.mediator.api.McpTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/** Stateless transport: no session or handshake needed; tools, resources and prompts still work. */
@SpringBootTest(classes = StatelessHttpTest.App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mcp.mediator.enabled=true", "mcp.mediator.transport=STATELESS_HTTP"})
class StatelessHttpTest {
    @EnableAutoConfiguration
    @Import(App.Beans.class)
    static class App {
        @Service
        static class Beans {
            @McpTool(name = "echo", description = "echo")
            public String echo(String text) {
                return "echo:" + text;
            }

            @McpResource(uri = "mem://r", name = "r")
            public String r() {
                return "res";
            }

            @org.springaicommunity.mcp.annotation.McpPrompt(name = "p", description = "d")
            public String p(@org.springaicommunity.mcp.annotation.McpArg(name = "x") String x) {
                return "prompt " + x;
            }
        }
    }

    @Value("${local.server.port}")
    int port;

    private JsonNode rpc(String method, String params) throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                .header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method
                        + "\",\"params\":" + params + "}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode(), r.body());
        return new ObjectMapper().readTree(r.body()).get("result");
    }

    @Test
    void toolsResourcesAndPromptsWorkWithoutSessions() throws Exception {
        assertEquals("echo:hi", rpc("tools/call", "{\"name\":\"echo\",\"arguments\":{\"text\":\"hi\"}}")
                .get("content").get(0).get("text").asText().replace("\"", ""));
        assertEquals("res", rpc("resources/read", "{\"uri\":\"mem://r\"}").get("contents").get(0).get("text").asText());
        assertEquals("prompt z", rpc("prompts/get", "{\"name\":\"p\",\"arguments\":{\"x\":\"z\"}}")
                .get("messages").get(0).get("content").get("text").asText());
    }
}
