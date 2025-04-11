package io.github.makbn.mcp.mediator.integration.core;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpRequest;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CoreIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private McpMediator mediator;

    @Test
    void testMediatorLifecycle() {
        assertFalse(mediator.isRunning(), "Mediator should not be running initially");
        
        mediator.start();
        assertTrue(mediator.isRunning(), "Mediator should be running after start");
        
        mediator.stop();
        assertFalse(mediator.isRunning(), "Mediator should not be running after stop");
    }

    @Test
    void testRequestHandlerRegistration() {
        McpRequestHandler<TestRequest> handler = new TestRequestHandler();
        
        mediator.registerHandler(handler);
        assertTrue(mediator.getHandlers().contains(handler), "Handler should be registered");
        
        mediator.unregisterHandler(handler);
        assertFalse(mediator.getHandlers().contains(handler), "Handler should be unregistered");
    }

    @Test
    void testRequestExecution() {
        TestRequestHandler handler = new TestRequestHandler();
        mediator.registerHandler(handler);
        
        TestRequest request = new TestRequest("test");
        Object result = mediator.execute(request);
        
        assertEquals("test", result, "Request execution should return correct result");
    }

    private static class TestRequest implements McpRequest {
        private final String value;

        TestRequest(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return "test.request";
        }

        @Override
        public String getImplementationName() {
            return "test";
        }

        public String getValue() {
            return value;
        }
    }

    private static class TestRequestHandler implements McpRequestHandler<TestRequest> {
        @Override
        public String getRequestType() {
            return "test.request";
        }

        @Override
        public String getImplementationName() {
            return "test";
        }

        @Override
        public boolean canHandle(McpRequest request) {
            return request instanceof TestRequest;
        }

        @Override
        public Object handle(TestRequest request) {
            return request.getValue();
        }

        @Override
        public TestRequest createRequest(Map<String, Object> params) {
            return new TestRequest((String) params.get("value"));
        }
    }
} 