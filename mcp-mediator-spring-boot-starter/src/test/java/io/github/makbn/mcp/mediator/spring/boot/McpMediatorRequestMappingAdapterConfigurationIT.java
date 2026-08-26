package io.github.makbn.mcp.mediator.spring.boot;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = McpMediatorRequestMappingAdapterConfigurationIT.TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class McpMediatorRequestMappingAdapterConfigurationIT {

    @SpringBootApplication
    @Import(McpMediatorRequestMappingAdapterConfiguration.class)
    static class TestApplication {
        
        @RestController
        static class TestController {
            @GetMapping("/test")
            public String testEndpoint() {
                return "OK";
            }
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private McpMediatorRequestMappingAdapterConfiguration configuration;

    @Test
    void testMediatorIsInitializedWithControllerEndpoints() {
        McpMediator mcpMediator = configuration.getMcpMediator();
        
        assertNotNull(mcpMediator, "McpMediator should be initialized upon context refresh");
        
        boolean hasSpringControllerHandler = false;
        for (McpMediatorRequestHandler handler : mcpMediator.getHandlers()) {
            if ("spring_controllers_mcp_server".equals(handler.getName())) {
                hasSpringControllerHandler = true;
                break;
            }
        }
        assertTrue(hasSpringControllerHandler, "Should have registered the spring controller handler");
    }
}
