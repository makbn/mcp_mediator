package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpClientHandler;
import io.github.makbn.mcp.mediator.api.McpMeta;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.github.makbn.mcp.mediator.core.McpClientHandlers;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live round trip: a real MCP SDK client talks to the Spring Boot server over streamable HTTP. The server tool asks
 * the client for an LLM completion (sampling); the client answers through annotated handlers, in both annotation
 * families. Also covers {@code McpMeta} injection and multi-argument handler signatures.
 */
@SpringBootTest(classes = LiveClientRoundTripTest.App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mcp.mediator.enabled=true", "mcp.mediator.transport=STREAMABLE_HTTP"})
class LiveClientRoundTripTest {

    @EnableAutoConfiguration
    @Import(App.Tools.class)
    static class App {
        @Service
        static class Tools {
            @McpTool(name = "ask", description = "asks the client's LLM")
            public String ask(McpSyncServerExchange exchange, String question) {
                McpSchema.CreateMessageResult r = exchange.createMessage(McpSchema.CreateMessageRequest.builder()
                        .messages(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER,
                                new McpSchema.TextContent(question))))
                        .maxTokens(20).build());
                return ((McpSchema.TextContent) r.content()).text();
            }

            @McpTool(name = "meta", description = "reads request meta")
            public String meta(McpMeta meta) {
                return String.valueOf(meta.get("trace"));
            }
        }
    }

    public static class OwnClientHandlers {
        final List<String> logs = new CopyOnWriteArrayList<>();

        @McpClientHandler(McpClientHandler.Kind.SAMPLING)
        public McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest r) {
            String q = ((McpSchema.TextContent) r.messages().get(0).content()).text();
            return McpSchema.CreateMessageResult.builder().message("answer to " + q).model("test").build();
        }
    }

    public static class SpringAiClientHandlers {
        @org.springaicommunity.mcp.annotation.McpSampling(clients = "c")
        public McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest r) {
            String q = ((McpSchema.TextContent) r.messages().get(0).content()).text();
            return McpSchema.CreateMessageResult.builder().message("spring answer to " + q).model("test").build();
        }
    }

    @Value("${local.server.port}")
    int port;

    private McpSyncClient client(Object handlers) {
        McpClient.SyncSpec spec = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp").jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper())).build());
        McpClientHandlers.scan(List.of(handlers)).applyTo(spec);
        McpSyncClient c = spec.capabilities(McpSchema.ClientCapabilities.builder().sampling().build()).build();
        c.initialize();
        return c;
    }

    @Test
    void serverSamplesThroughOwnAnnotatedClientHandler() {
        try (McpSyncClient c = client(new OwnClientHandlers())) {
            McpSchema.CallToolResult r = c.callTool(new McpSchema.CallToolRequest("ask", java.util.Map.of("question", "2+2")));
            assertFalse(Boolean.TRUE.equals(r.isError()), r.toString());
            assertTrue(((McpSchema.TextContent) r.content().get(0)).text().contains("answer to 2+2"), r.toString());
        }
    }

    @Test
    void serverSamplesThroughSpringAiAnnotatedClientHandler() {
        try (McpSyncClient c = client(new SpringAiClientHandlers())) {
            McpSchema.CallToolResult r = c.callTool(new McpSchema.CallToolRequest("ask", java.util.Map.of("question", "hi")));
            assertFalse(Boolean.TRUE.equals(r.isError()), r.toString());
            assertTrue(((McpSchema.TextContent) r.content().get(0)).text().contains("spring answer to hi"), r.toString());
        }
    }

    @Test
    void metaIsInjected() {
        try (McpSyncClient c = client(new OwnClientHandlers())) {
            McpSchema.CallToolResult r = c.callTool(McpSchema.CallToolRequest.builder().name("meta")
                    .arguments(java.util.Map.of()).meta(java.util.Map.of("trace", "abc")).build());
            assertTrue(((McpSchema.TextContent) r.content().get(0)).text().contains("abc"), r.toString());
        }
    }
}
