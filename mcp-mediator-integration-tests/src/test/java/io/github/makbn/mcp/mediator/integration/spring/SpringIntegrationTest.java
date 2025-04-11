package io.github.makbn.mcp.mediator.integration.spring;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpRequest;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SpringIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private McpMediator mediator;

    @Test
    void testMediatorBean() {
        assertNotNull(mediator, "Mediator bean should be available");
        assertTrue(applicationContext.containsBean("mcpMediator"), 
                "Mediator bean should be registered in Spring context");
    }

    @Test
    void testRequestHandlerAutoRegistration() {
        TestRequestHandler handler = new TestRequestHandler();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(handler);
        
        mediator.registerHandler(handler);
        assertTrue(mediator.getHandlers().contains(handler), 
                "Handler should be registered in mediator");
    }

    private static class TestRequest implements McpRequest {
        @Override
        public String getType() {
            return "spring.test.request";
        }

        @Override
        public String getImplementationName() {
            return "spring";
        }
    }

    private static class TestRequestHandler implements McpRequestHandler<TestRequest> {
        @Override
        public String getRequestType() {
            return "spring.test.request";
        }

        @Override
        public String getImplementationName() {
            return "spring";
        }

        @Override
        public boolean canHandle(McpRequest request) {
            return request instanceof TestRequest;
        }

        @Override
        public Object handle(TestRequest request) {
            return "spring-test-result";
        }

        @Override
        public TestRequest createRequest(Map<String, Object> params) {
            return new TestRequest();
        }
    }
} 