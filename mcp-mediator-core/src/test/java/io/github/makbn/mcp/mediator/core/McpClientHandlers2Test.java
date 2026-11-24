package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.api.McpClientHandler;
import io.github.makbn.mcp.mediator.api.McpClientHandler.Kind;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McpClientHandlers2Test {
    static final List<String> seen = new ArrayList<>();

    public static class Own {
        @McpClientHandler(Kind.SAMPLING)
        public McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest r) {
            return McpSchema.CreateMessageResult.builder().message("own").model("m").build();
        }

        @McpClientHandler(Kind.LOGGING)
        public void log(McpSchema.LoggingMessageNotification n) {
            seen.add("own-log:" + n.data());
        }

        @McpClientHandler(Kind.TOOLS_CHANGED)
        public void tools(List<McpSchema.Tool> tools) {
            seen.add("own-tools:" + tools.size());
        }
    }

    public static class SpringAi {
        @McpSampling(clients = "x")
        public McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest r) {
            return McpSchema.CreateMessageResult.builder().message("spring").model("m").build();
        }

        @McpElicitation(clients = "x")
        public McpSchema.ElicitResult elicit(McpSchema.ElicitFormRequest r) {
            return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, java.util.Map.of("a", 1));
        }

        @McpLogging(clients = "x")
        public void log(McpSchema.LoggingMessageNotification n) {
            seen.add("spring-log:" + n.data());
        }

        @McpProgress(clients = "x")
        public void progress(McpSchema.ProgressNotification n) {
            seen.add("spring-progress:" + n.progress());
        }
    }

    private static McpSchema.CreateMessageRequest request() {
        return McpSchema.CreateMessageRequest.builder(List.of(), 10).build();
    }

    @Test
    void bothFamiliesAreDiscoveredAndInvoked() {
        var own = McpClientHandlers.scan(List.of(new Own()));
        var spring = McpClientHandlers.scan(List.of(new SpringAi()));

        assertEquals("own", ((McpSchema.TextContent) own.sampling().apply(request()).content()).text());
        assertEquals("spring", ((McpSchema.TextContent) spring.sampling().apply(request()).content()).text());
        assertEquals(McpSchema.ElicitResult.Action.ACCEPT, spring.elicitation().apply(null).action());

        own.logging().forEach(c -> c.accept(new McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.INFO, "l", "own")));
        spring.logging().forEach(c -> c.accept(new McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.INFO, "l", "spring")));
        spring.progress().forEach(c -> c.accept(new McpSchema.ProgressNotification("t", 0.5, 1.0, "m")));
        own.toolsChanged().forEach(c -> c.accept(List.of()));
        assertEquals(List.of("own-log:own", "spring-log:spring", "spring-progress:0.5", "own-tools:0"), seen);
    }

    @Test
    void handlersAreAppliedToTheClientBuilder() {
        var transport = new StdioClientTransport(ServerParameters.builder("true").build(),
                new JacksonMcpJsonMapper(new com.fasterxml.jackson.databind.ObjectMapper()));
        var handlers = McpClientHandlers.scan(List.of(new SpringAi(), new Own()));
        assertTrue(handlers.has("SAMPLING") && handlers.has("ELICITATION") && handlers.has("PROGRESS"));
        assertDoesNotThrow(() -> handlers.applyTo(McpClient.sync(transport)));
        assertTrue(McpClientHandlers.scan(List.of(new Object())).isEmpty());
    }

    public static class MultiArg {
        @McpLogging(clients = "x")
        public void log(McpSchema.LoggingLevel level, String logger, String data) {
            seen.add("multi-log:" + level + "/" + logger + "/" + data);
        }

        @McpProgress(clients = "x")
        public void progress(@McpProgressToken String token, Double progress, Double total, String message) {
            seen.add("multi-progress:" + token + "/" + progress + "/" + total + "/" + message);
        }
    }

    @Test
    void multiArgumentSignaturesAreBound() {
        seen.clear();
        var h = McpClientHandlers.scan(List.of(new MultiArg()));
        h.logging().forEach(c -> c.accept(new McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.WARNING, "lg", "boom")));
        h.progress().forEach(c -> c.accept(new McpSchema.ProgressNotification("tok", 1.0, 4.0, "step")));
        assertEquals(List.of("multi-log:WARNING/lg/boom", "multi-progress:tok/1.0/4.0/step"), seen);
    }

    public static class Routed {
        @McpLogging(clients = "a")
        public void forA(McpSchema.LoggingMessageNotification n) {
        }

        @McpClientHandler(value = Kind.LOGGING, clients = {"b", "c"})
        public void forBC(McpSchema.LoggingMessageNotification n) {
        }

        @McpClientHandler(Kind.LOGGING)
        public void forAll(McpSchema.LoggingMessageNotification n) {
        }
    }

    @Test
    void clientsAttributeRoutesHandlersToNamedConnections() {
        var routed = List.of(new Routed());
        assertEquals(2, McpClientHandlers.scan(routed, "a").logging().size());
        assertEquals(2, McpClientHandlers.scan(routed, "b").logging().size());
        assertEquals(1, McpClientHandlers.scan(routed, "zzz").logging().size());
        assertEquals(3, McpClientHandlers.scan(routed).logging().size());
    }
}
