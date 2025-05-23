package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.*;
import io.github.makbn.mcp.mediator.core.adaper.McpMethodAdapter;
import io.github.makbn.mcp.mediator.core.internal.McpMethodArgumentResolver;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "java:S1452"})
@RequiredArgsConstructor(staticName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class McpServiceFactory {

    public static final String ERROR_PREFIX = "Can't find the adapter for this request: ";

    public abstract static class McpServiceRequestHandler implements McpMediatorRequestHandler {
        protected McpServiceRequestHandler() {
            super();
        }

        public abstract Map<? extends McpServiceRequest, McpMethodAdapter> getAdapterMap();
    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class McpServiceRequest extends HashMap<String, Object> implements McpMediatorRequest<Object> {
        private final String uuid;
        private String name;
        private String description;

        public McpServiceRequest() {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    Object service;
    Set<String> excludedMethods = new HashSet<>();

    @NonFinal
    boolean createForNonAnnotatedMethods = false;


    public McpServiceFactory createForNonAnnotatedMethods(boolean createForNonAnnotatedMethods) {
        this.createForNonAnnotatedMethods = createForNonAnnotatedMethods;
        return this;
    }

    public McpServiceFactory excludeMethod(@NonNull String methodName) {
        this.excludedMethods.add(methodName);
        return this;
    }

    public McpServiceFactory excludeMethods(@NonNull Collection<String> methodName) {
        this.excludedMethods.addAll(methodName);
        return this;
    }

    public McpMediatorRequestHandler<?, ?> build() {
        if (!service.getClass().isAnnotationPresent(McpService.class)) {
            throw new McpMediatorException("Service class should be annotated with @McpService");
        }
        McpService serviceAnnotation = Objects.requireNonNull(service.getClass().getAnnotation(McpService.class));

        return createServiceHandler(excludedMethods, createForNonAnnotatedMethods, service, serviceAnnotation);
    }


    private static McpServiceRequestHandler createServiceHandler(Set<String> excludedMethods,
            boolean createForNonAnnotated, Object service, McpService serviceAnnotation) {
        return new McpServiceRequestHandler() {
            private static final Logger log = LoggerFactory.getLogger("McpServiceRequestHandler");
            private Map<? extends McpServiceRequest, McpMethodAdapter> adapterMap;
            private Object internalService;

            @Override
            @SuppressWarnings("java:S3864")
            public void initialize(Object[] args) {
                ObjectMapper mapper = getObjectMapper(args);
                this.internalService = service;
                this.adapterMap = Arrays.stream(service.getClass().getDeclaredMethods())
                        .filter(method -> createForNonAnnotated || method.isAnnotationPresent(McpTool.class))
                        .filter(method -> !excludedMethods.contains(method.getName()))
                        .map(method -> new McpMethodAdapter(method, mapper))
                        .peek(adapter -> log.debug("Mapped method: {} to: {}", adapter.getMethod(), adapter.getSourceTool()))
                        .collect(Collectors.toMap(this::convertToRequest, Function.identity()));
            }

            @Override
            public String getName() {
                return serviceAnnotation.name();
            }

            @Override
            public Object handle(McpMediatorRequest request) throws McpMediatorException {
                McpServiceRequest mcpServiceRequest = (McpServiceRequest) request;
                McpMethodAdapter adapter = findAdapter(request);

                Object[] parameters = McpMethodArgumentResolver.resolveArguments(
                        adapter.getSourceTool(), mcpServiceRequest);
                try {
                    return adapter.getSourceTool().invoke(internalService, parameters);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new McpMediatorException(generateMessage(e, adapter), e);
                }

            }

            private McpMethodAdapter findAdapter(McpMediatorRequest request) {
                return adapterMap.entrySet().stream()
                        .filter(entry ->
                                entry.getKey().getClass().equals(request.getClass()) || entry.getKey().getClass().isAssignableFrom(request.getClass()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(() -> new McpMediatorException(ERROR_PREFIX + request.getClass()));
            }

            @Override
            @SuppressWarnings("java:S6204")
            public Collection<Class<? extends McpMediatorRequest>> getAllSupportedRequestClass() {
                return adapterMap.keySet().stream().map(McpServiceRequest::getClass).collect(Collectors.toList());
            }

            @Override
            public boolean canHandle(McpMediatorRequest request) {
                return adapterMap.keySet().stream()
                        .anyMatch(requestClass ->
                                requestClass.getClass().equals(request.getClass()) || requestClass.getClass().isAssignableFrom(request.getClass()));
            }

            @Override
            public Map<? extends McpServiceRequest, McpMethodAdapter> getAdapterMap() {
                return adapterMap;
            }

            private McpServiceRequest convertToRequest(McpMethodAdapter adapter) {
                McpServiceRequest request;
                try (DynamicType.Unloaded<McpServiceRequest> unloaded = new ByteBuddy(ClassFileVersion.JAVA_V17)
                        .subclass(McpServiceRequest.class)
                        .name("generated." + adapter.getMethod() + "_Request")
                        .make()) {
                    Class<? extends McpServiceRequest> requestClass = unloaded.load(getClass().getClassLoader(),
                                    ClassLoadingStrategy.Default.INJECTION)
                            .getLoaded();

                    request = requestClass.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                         IllegalAccessException e) {
                    throw new McpMediatorException("Failed to create request class for: " + adapter.getMethod(), e);
                }

                request.setName(adapter.getMethod());
                request.setDescription(adapter.getDescription());

                return request;
            }

            private ObjectMapper getObjectMapper(Object[] args) {
                ObjectMapper mapper;
                if (args[0] instanceof ObjectMapper objectMapper) {
                    mapper = objectMapper;
                } else {
                    throw new McpMediatorException("ObjectMapper is required for McpService initialization");
                }
                return mapper;
            }
        };
    }

    private static String generateMessage(Exception e, McpMethodAdapter adapter) {
        StringBuilder cause = new StringBuilder();
        Throwable causeException = e;
        while (causeException != null) {
            if (causeException.getMessage() != null && !causeException.getMessage().isBlank()) {
                cause.insert(0, String.format("%n%s %s", "->", causeException.getMessage().trim()));
            }else {
                cause.insert(0, String.format("%n%s %s", "->", causeException.getClass().getSimpleName()));
            }
            causeException =  causeException.getCause();
        }
        return "Failed to invoke method for [" + adapter.getMethod() + "]:" + cause;
    }
}
