package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import io.github.makbn.mcp.mediator.core.McpServiceFactory.McpServiceRequest;
import io.github.makbn.mcp.mediator.core.McpServiceFactory.McpServiceRequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpSpringControllerFactoryTest {

    static class DummyController {
        @GetMapping("/hello")
        public String sayHello(@RequestParam String name) {
            return "Hello " + name;
        }
    }

    private DummyController dummyController;
    private Method dummyMethod;
    private Map<RequestMappingInfo, HandlerMethod> map;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        dummyController = new DummyController();
        dummyMethod = DummyController.class.getMethod("sayHello", String.class);
        HandlerMethod handlerMethod = new HandlerMethod(dummyController, dummyMethod);
        
        map = new HashMap<>();
        map.put(RequestMappingInfo.paths("/hello").build(), handlerMethod);
    }

    @Test
    void testBuildAndInitialize() {
        McpSpringControllerFactory factory = McpSpringControllerFactory.create(map);
        McpMediatorRequestHandler<?, ?> handler = factory.build();
        
        // Initialize the handler with an ObjectMapper
        assertDoesNotThrow(() -> handler.initialize(new Object[]{new ObjectMapper()}));

        assertEquals("spring_controllers_mcp_server", handler.getName());
        
        Collection<?> requestClasses = handler.getAllSupportedRequestClass();
        assertEquals(1, requestClasses.size());

        // Verify the prototype request stored in the adapter map has the correct name
        Map<?, ?> adapterMap = ((McpServiceRequestHandler) handler).getAdapterMap();
        McpServiceRequest prototypeRequest = (McpServiceRequest) adapterMap.keySet().iterator().next();
        assertEquals("say_hello", prototypeRequest.getName());

        // Find the generated request class and invoke it
        Class<? extends McpServiceRequest> generatedClass = (Class<? extends McpServiceRequest>) requestClasses.iterator().next();
        
        try {
            McpServiceRequest request = generatedClass.getDeclaredConstructor().newInstance();
            request.setName(prototypeRequest.getName());
            
            // Check canHandle
            assertTrue(handler.canHandle(request));
            
            // Invoke the method via handle
            request.put("name", "World");
            Object result = ((McpMediatorRequestHandler) handler).handle(request);
            
            assertEquals("Hello World", result);
        } catch (Exception e) {
            fail("Exception thrown during request handling", e);
        }
    }
}
