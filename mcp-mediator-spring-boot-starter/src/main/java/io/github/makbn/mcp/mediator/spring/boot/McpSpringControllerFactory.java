package io.github.makbn.mcp.mediator.spring.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.*;
import io.github.makbn.mcp.mediator.core.McpServiceFactory.McpServiceRequest;
import io.github.makbn.mcp.mediator.core.McpServiceFactory.McpServiceRequestHandler;
import io.github.makbn.mcp.mediator.core.adaper.McpMethodAdapter;
import io.github.makbn.mcp.mediator.core.internal.McpMethodArgumentResolver;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "java:S1452"})
public class McpSpringControllerFactory {

    private final Map<RequestMappingInfo, HandlerMethod> handlerMethods;

    private McpSpringControllerFactory(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        this.handlerMethods = handlerMethods;
    }

    public static McpSpringControllerFactory create(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        return new McpSpringControllerFactory(handlerMethods);
    }

    public McpMediatorRequestHandler<?, ?> build() {
        return createControllerHandler(handlerMethods);
    }

    private static McpServiceRequestHandler createControllerHandler(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        return new McpServiceRequestHandler() {
            private static final Logger log = LoggerFactory.getLogger("McpSpringControllerRequestHandler");
            private Map<? extends McpServiceRequest, ControllerMethodContext> adapterMap;

            record ControllerMethodContext(McpMethodAdapter adapter, HandlerMethod handlerMethod) {}

            @Override
            public void initialize(Object[] args) {
                ObjectMapper mapper = getObjectMapper(args);
                
                this.adapterMap = handlerMethods.entrySet().stream()
                        .map(entry -> {
                            Method method = entry.getValue().getMethod();
                            McpMethodAdapter adapter = new McpMethodAdapter(method, mapper);
                            return new ControllerMethodContext(adapter, entry.getValue());
                        })
                        .collect(Collectors.toMap(ctx -> convertToRequest(ctx.adapter()), Function.identity()));
            }

            @Override
            public String getName() {
                return "spring_controllers_mcp_server";
            }

            @Override
            public Object handle(McpMediatorRequest request) throws McpMediatorException {
                McpServiceRequest mcpServiceRequest = (McpServiceRequest) request;
                ControllerMethodContext context = findContext(request);
                McpMethodAdapter adapter = context.adapter();

                Object[] parameters = McpMethodArgumentResolver.resolveArguments(
                        adapter.getSourceTool(), mcpServiceRequest);
                try {
                    Object bean = context.handlerMethod().getBean();
                    if (bean instanceof String) {
                        throw new McpMediatorException("Bean is not resolved: " + bean);
                    }
                    return adapter.getSourceTool().invoke(bean, parameters);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new McpMediatorException(generateMessage(e, adapter), e);
                }
            }

            private ControllerMethodContext findContext(McpMediatorRequest request) {
                return adapterMap.entrySet().stream()
                        .filter(entry -> entry.getKey().getClass().isAssignableFrom(request.getClass()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(() -> new McpMediatorException("Can't find the adapter for this request: " + request.getClass()));
            }

            @Override
            public Collection<Class<? extends McpMediatorRequest>> getAllSupportedRequestClass() {
                return adapterMap.keySet().stream().map(McpServiceRequest::getClass).collect(Collectors.toList());
            }

            @Override
            public boolean canHandle(McpMediatorRequest request) {
                return adapterMap.keySet().stream()
                        .anyMatch(requestClass -> requestClass.getClass().isAssignableFrom(request.getClass()));
            }

            @Override
            public Map<? extends McpServiceRequest, McpMethodAdapter> getAdapterMap() {
                return adapterMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().adapter()));
            }

            private McpServiceRequest convertToRequest(McpMethodAdapter adapter) {
                McpServiceRequest request;
                try (DynamicType.Unloaded<McpServiceRequest> unloaded = new ByteBuddy(ClassFileVersion.JAVA_V21)
                        .subclass(McpServiceRequest.class)
                        .name("generated." + adapter.getMethod() + "_Request")
                        .make()) {
                    Class<? extends McpServiceRequest> requestClass = unloaded.load(getClass().getClassLoader(),
                                    ClassLoadingStrategy.Default.INJECTION)
                            .getLoaded();

                    request = requestClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new McpMediatorException("Failed to create request class for: " + adapter.getMethod(), e);
                }

                request.setName(adapter.getMethod());
                request.setDescription(adapter.getDescription());
                return request;
            }

            private ObjectMapper getObjectMapper(Object[] args) {
                if (args != null && args.length > 0 && args[0] instanceof ObjectMapper objectMapper) {
                    return objectMapper;
                }
                throw new McpMediatorException("ObjectMapper is required for initialization");
            }
        };
    }

    private static String generateMessage(Exception e, McpMethodAdapter adapter) {
        return "Failed to invoke method for [" + adapter.getMethod() + "]: " + e.getMessage();
    }
}
