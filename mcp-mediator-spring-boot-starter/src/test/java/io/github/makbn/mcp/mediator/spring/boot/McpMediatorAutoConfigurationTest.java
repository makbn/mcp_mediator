package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpPrompt;
import io.github.makbn.mcp.mediator.api.McpResource;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Timeout(20)
class McpMediatorAutoConfigurationTest {

    /** Spring bean using the mediator's own annotations; dependencies are injected like any Spring bean. */
    @Service
    static class OwnAnnotatedBean {
        private final Greeter greeter;

        OwnAnnotatedBean(Greeter greeter) {
            this.greeter = greeter;
        }

        @McpTool(name = "greet", description = "greets")
        public String greet(String who) {
            return greeter.greet(who);
        }

        @McpResource(uri = "mem://hello", name = "hello")
        public String hello() {
            return "hi";
        }
    }

    /** Spring bean using Spring AI's native annotations only. */
    @Component
    static class SpringAiAnnotatedBean {
        @org.springaicommunity.mcp.annotation.McpTool(name = "shout", description = "shouts")
        public String shout(String text) {
            return text.toUpperCase();
        }

        @org.springaicommunity.mcp.annotation.McpPrompt(name = "p", description = "d")
        public String prompt(@org.springaicommunity.mcp.annotation.McpArg(name = "topic") String topic) {
            return "about " + topic;
        }
    }

    /** Proxied by Spring: CGLIB subclass hides nothing from the scanner. */
    @Service
    static class ProxiedBean {
        @McpTool(name = "proxied", description = "x")
        public String proxied() {
            return "ok";
        }
    }

    @Component
    static class Greeter {
        String greet(String who) {
            return "Hello " + who;
        }
    }

    @Configuration
    static class Beans {
        static PipedOutputStream clientOut;
        static BufferedReader clientIn;

        @Bean
        McpMediator customMediator() throws IOException {
            clientOut = new PipedOutputStream();
            PipedOutputStream serverOut = new PipedOutputStream();
            clientIn = new BufferedReader(new InputStreamReader(new PipedInputStream(serverOut, 1 << 16)));
            return new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder().createDefault()
                    .stdioInputStream(new PipedInputStream(clientOut, 1 << 16)).stdioOutputStream(serverOut).build());
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(McpMediatorAutoConfiguration.class))
            .withUserConfiguration(OwnAnnotatedBean.class, SpringAiAnnotatedBean.class, Greeter.class, Beans.class)
            .withPropertyValues("mcp.mediator.enabled=true");

    @Test
    void disabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(McpMediatorAutoConfiguration.class))
                .run(ctx -> assertTrue(ctx.getBeansOfType(McpMediator.class).isEmpty()));
    }

    @Test
    void backsOffWhenApplicationDefinesItsOwnMediator() {
        runner.run(ctx -> {
            assertEquals(1, ctx.getBeansOfType(McpMediator.class).size());
            assertSame(ctx.getBean("customMediator"), ctx.getBean(McpMediator.class));
        });
    }

    @Test
    void registersBeansOfBothAnnotationFamilies() {
        runner.run(ctx -> {
            McpMediator mediator = ctx.getBean(McpMediator.class);
            mediator.initialize();
            Rpc rpc = new Rpc();
            rpc.handshake();

            String tools = rpc.request("tools/list", "{}").get("tools").toString();
            assertTrue(tools.contains("\"greet\"") && tools.contains("\"shout\""), tools);

            // dependency injection worked in the bean the mediator invokes
            assertEquals("\"Hello Bob\"".replace("\"", ""),
                    rpc.request("tools/call", "{\"name\":\"greet\",\"arguments\":{\"who\":\"Bob\"}}")
                            .get("content").get(0).get("text").asText().replace("\"", ""));
            assertEquals("hi", rpc.request("resources/read", "{\"uri\":\"mem://hello\"}")
                    .get("contents").get(0).get("text").asText());
            assertEquals("about java", rpc.request("prompts/get",
                            "{\"name\":\"p\",\"arguments\":{\"topic\":\"java\"}}")
                    .get("messages").get(0).get("content").get("text").asText());
        });
    }

    @Test
    void proxiedBeansAreSupported() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(McpMediatorAutoConfiguration.class))
                .withUserConfiguration(ProxyConfig.class, Beans.class)
                .withPropertyValues("mcp.mediator.enabled=true")
                .run(ctx -> {
                    ctx.getBean(McpMediator.class).initialize();
                    Rpc rpc = new Rpc();
                    rpc.handshake();
                    assertTrue(rpc.request("tools/list", "{}").get("tools").toString().contains("proxied"));
                });
    }

    @Configuration
    static class ProxyConfig {
        @Bean
        ProxiedBean proxiedBean() {
            // force a CGLIB subclass proxy without needing a transaction manager
            org.springframework.aop.framework.ProxyFactory pf =
                    new org.springframework.aop.framework.ProxyFactory(new ProxiedBean());
            pf.setProxyTargetClass(true);
            return (ProxiedBean) pf.getProxy();
        }
    }

    /** Minimal JSON-RPC client over the piped stdio of {@link Beans}. */
    private static final class Rpc {
        private final ObjectMapper mapper = new ObjectMapper();
        private int id;

        void handshake() throws IOException {
            request("initialize", "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                    + "\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}");
            Beans.clientOut.write("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n".getBytes());
            Beans.clientOut.flush();
        }

        JsonNode request(String method, String params) throws IOException {
            int myId = ++id;
            Beans.clientOut.write(("{\"jsonrpc\":\"2.0\",\"id\":" + myId + ",\"method\":\"" + method
                    + "\",\"params\":" + params + "}\n").getBytes());
            Beans.clientOut.flush();
            String line;
            while ((line = Beans.clientIn.readLine()) != null) {
                JsonNode n = mapper.readTree(line);
                if (n.has("id") && n.get("id").asInt() == myId) {
                    assertFalse(n.has("error"), n.toString());
                    return n.get("result");
                }
            }
            throw new IllegalStateException("no response");
        }
    }
}
