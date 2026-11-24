package io.github.makbn.mcp.mediator.spring.boot;

import io.github.makbn.mcp.mediator.api.McpTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/** The SSE transport is mounted automatically: GET /sse opens an event stream announcing the message endpoint. */
@SpringBootTest(classes = SseTransportTest.App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mcp.mediator.enabled=true", "mcp.mediator.transport=SSE"})
class SseTransportTest {
    @EnableAutoConfiguration
    @Import(App.Tools.class)
    static class App {
        @Service
        static class Tools {
            @McpTool(name = "ping2", description = "x")
            public String ping2() {
                return "pong";
            }
        }
    }

    @Value("${local.server.port}")
    int port;

    @Test
    void sseEndpointStreamsEndpointEvent() throws Exception {
        HttpResponse<java.io.InputStream> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/sse"))
                        .header("Accept", "text/event-stream").GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").contains("text/event-stream"));
        try (BufferedReader in = new BufferedReader(new InputStreamReader(r.body()))) {
            String l1 = in.readLine();
            String l2 = in.readLine();
            assertEquals("event: endpoint", l1);
            assertTrue(l2.contains("/mcp/message"), l2);
        }
    }
}
