package demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoexistTest {
    @Autowired ApplicationContext ctx;

    @Test void bothStartersOnClasspathSpringAiServesAnnotatedTool() {
        var servers = ctx.getBeansOfType(io.modelcontextprotocol.server.McpSyncServer.class);
        assertEquals(1, servers.size(), "Spring AI's server");
        assertTrue(servers.values().iterator().next().listTools().stream().anyMatch(t -> t.name().equals("double_it")),
                "Spring AI registered the @McpTool bean");
        assertTrue(ctx.getBeansOfType(io.github.makbn.mcp.mediator.api.McpMediator.class).isEmpty(),
                "mediator auto-configuration is opt-in and stays out of the way");
    }
}
