package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Drives a {@link DefaultMcpMediator} as a real MCP client would: JSON-RPC lines over piped stdio. */
final class StdioHarness implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    final DefaultMcpMediator mediator;
    private final PrintWriter toServer;
    private final BufferedReader fromServer;
    private final AtomicInteger ids = new AtomicInteger();

    StdioHarness(java.util.function.Consumer<DefaultMcpMediator> setup) throws IOException {
        this(setup, "{}");
    }

    StdioHarness(java.util.function.Consumer<DefaultMcpMediator> setup, String clientCapabilities) throws IOException {
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut, 1 << 16);
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedInputStream clientIn = new PipedInputStream(serverOut, 1 << 16);
        toServer = new PrintWriter(new OutputStreamWriter(clientOut), true);
        fromServer = new BufferedReader(new InputStreamReader(clientIn));
        mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder().createDefault()
                .serverName("test").serverVersion("1.0")
                .stdioInputStream(serverIn).stdioOutputStream(serverOut).build());
        setup.accept(mediator);
        mediator.initialize();
        request("initialize", "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":" + clientCapabilities + ","
                + "\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}");
        toServer.println("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
    }

    JsonNode request(String method, String paramsJson) throws IOException {
        int id = ids.incrementAndGet();
        toServer.println("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"" + method + "\",\"params\":" + paramsJson + "}");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            String line = fromServer.readLine();
            if (line == null) break;
            JsonNode n = MAPPER.readTree(line);
            if (n.has("id") && n.get("id").asInt() == id) {
                if (n.has("error")) throw new IllegalStateException(n.get("error").toString());
                return n.get("result");
            }
        }
        throw new IllegalStateException("no response to " + method);
    }

    @Override
    public void close() {
        // the mediator closes its server on JVM exit; piped streams are collected with the test
    }
}
