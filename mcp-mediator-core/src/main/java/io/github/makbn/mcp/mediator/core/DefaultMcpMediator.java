package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.makbn.mcp.mediator.api.*;
import io.github.makbn.mcp.mediator.core.adaper.McpAdapterFactory;
import io.github.makbn.mcp.mediator.core.util.McpAnnotations;
import io.github.makbn.mcp.mediator.core.internal.McpMethodSchemaGenerator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorDefaultConfiguration;
import io.github.makbn.mcp.mediator.core.internal.McpRequestExecutor;
import io.github.makbn.mcp.mediator.core.internal.MinimalMcpMediator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import lombok.AccessLevel;
import lombok.NonNull;
import io.github.makbn.mcp.mediator.api.McpMediatorInterceptor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Default base implementation of the MCP Mediator for MCP Server.
 * Provides common functionality for request handling and state management.
 *
 * @author Matt Akbarian
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class DefaultMcpMediator implements McpMediator {
    @SuppressWarnings("rawtypes")
    Map<Class<? extends McpMediatorRequest<?>>, McpMediatorRequestHandler> handlersMap = new ConcurrentHashMap<>();
    @SuppressWarnings("rawtypes")
    List<McpMediatorRequestHandler> handlersList = Collections.synchronizedList(new ArrayList<>());
    McpMediatorDefaultConfiguration configuration;
    AtomicBoolean initialized = new AtomicBoolean(false);

    List<McpFeatureScanner.Features> pendingFeatures = Collections.synchronizedList(new ArrayList<>());

    @NonFinal
    ExecutorService executorService;
    @NonFinal
    McpSyncServer mcpSyncServer;
    @NonFinal
    io.modelcontextprotocol.server.McpStatelessSyncServer statelessServer;
    @NonFinal
    Object transportProvider;
    List<Object> modernBeans = Collections.synchronizedList(new ArrayList<>());
    ModernDelegatingServlet modernServlet = new ModernDelegatingServlet();

    public DefaultMcpMediator() {
        this(McpMediatorConfigurationBuilder.builder().createDefault().build());
    }

    public DefaultMcpMediator(@NonNull McpMediatorDefaultConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Registers a request handler capable of handling one or more MCP request types.
     *
     * @param handler the request handler to register
     * @param <T>     the request type
     * @param <R>     the result type
     */
    @Override
    public <T extends McpMediatorRequest<R>, R> void registerHandler(@NonNull McpMediatorRequestHandler<T, R> handler) {
        handlersList.add(handler);
        // if server already initialized, add the handler to the map and register it to the server
        if (initialized.get()) {
            addToHandlersMap(handler);
            handler.getAllSupportedRequestClass().forEach(requestType ->
                    startHandlerToMcpToolConnection(requestType, handler, true));
        }
    }

    /** Mounted before initialize(); forwards to the modern server once it is built. */
    private static final class ModernDelegatingServlet extends jakarta.servlet.http.HttpServlet {
        private volatile jakarta.servlet.http.HttpServlet delegate;

        @Override
        protected void service(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse resp)
                throws jakarta.servlet.ServletException, java.io.IOException {
            jakarta.servlet.http.HttpServlet d = delegate;
            if (d == null) {
                resp.sendError(503, "MCP mediator not initialized");
            } else {
                d.service(req, resp);
            }
        }
    }

    @Override
    public void registerBean(@NonNull Object bean) {
        if (configuration.getTransportType() == io.github.makbn.mcp.mediator.api.McpTransportType.MODERN_HTTP) {
            modernBeans.add(bean);
            return;
        }
        if (McpServiceFactory.hasTools(bean)) {
            registerHandler(McpServiceFactory.create(bean).build());
        }
        McpFeatureScanner.Features features = McpFeatureScanner.scan(bean, configuration.getSerializer());
        if (features.isEmpty()) {
            return;
        }
        synchronized (pendingFeatures) {
            if (!initialized.get() && mcpSyncServer == null && statelessServer == null) {
                pendingFeatures.add(features);
                return;
            }
        }
        features.resources().forEach(this::serverAddResource);
        features.resourceTemplates().forEach(this::serverAddResourceTemplate);
        features.prompts().forEach(this::serverAddPrompt);
        if (!features.completions().isEmpty()) {
            log.warn("Completions registered after initialize() are ignored; register the bean before initialize()");
        }
        if (mcpSyncServer != null) {
            mcpSyncServer.notifyResourcesListChanged();
            mcpSyncServer.notifyPromptsListChanged();
        }
    }

    // ---- stateful / stateless server dispatch ------------------------------------------------------------------

    private void serverAddTool(McpServerFeatures.SyncToolSpecification t) {
        if (statelessServer != null) {
            statelessServer.addTool(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification(
                    t.tool(), (ctx, req) -> t.callHandler().apply(null, req)));
        } else {
            mcpSyncServer.addTool(t);
        }
    }

    private void serverAddResource(McpServerFeatures.SyncResourceSpecification r) {
        if (statelessServer != null) {
            statelessServer.addResource(statelessResource(r));
        } else {
            mcpSyncServer.addResource(r);
        }
    }

    private void serverAddResourceTemplate(McpServerFeatures.SyncResourceTemplateSpecification r) {
        if (statelessServer != null) {
            statelessServer.addResourceTemplate(statelessTemplate(r));
        } else {
            mcpSyncServer.addResourceTemplate(r);
        }
    }

    private void serverAddPrompt(McpServerFeatures.SyncPromptSpecification p) {
        if (statelessServer != null) {
            statelessServer.addPrompt(statelessPrompt(p));
        } else {
            mcpSyncServer.addPrompt(p);
        }
    }

    private static io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification statelessResource(
            McpServerFeatures.SyncResourceSpecification r) {
        return new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification(
                r.resource(), (ctx, req) -> r.readHandler().apply(null, req));
    }

    private static io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification statelessTemplate(
            McpServerFeatures.SyncResourceTemplateSpecification r) {
        return new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                r.resourceTemplate(), (ctx, req) -> r.readHandler().apply(null, req));
    }

    private static io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification statelessPrompt(
            McpServerFeatures.SyncPromptSpecification p) {
        return new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification(
                p.prompt(), (ctx, req) -> p.promptHandler().apply(null, req));
    }

    private static io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification statelessCompletion(
            McpServerFeatures.SyncCompletionSpecification c) {
        return new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification(
                c.referenceKey(), (ctx, req) -> c.completionHandler().apply(null, req));
    }

    /**
     * Initializes the mediator, creating the internal MCP server, and registering all known tools.
     *
     * @throws McpMediatorException if initialization fails
     */
    @Override
    public void initialize() throws McpMediatorException {
        log.info("Initializing MCP Mediator");
        try {
            executorService = Executors.newCachedThreadPool();
            if (configuration.getTransportType() == io.github.makbn.mcp.mediator.api.McpTransportType.MODERN_HTTP) {
                McpModernServer modern = new McpModernServer(new ArrayList<>(modernBeans), configuration.getSerializer(),
                        configuration.getServerName(), configuration.getServerVersion(), "/mcp");
                jakarta.servlet.http.HttpServlet servlet = modern.servlet();
                servlet.init(new jakarta.servlet.ServletConfig() {
                    public String getServletName() { return "mcp-modern"; }
                    public jakarta.servlet.ServletContext getServletContext() { return null; }
                    public String getInitParameter(String name) { return null; }
                    public java.util.Enumeration<String> getInitParameterNames() { return Collections.emptyEnumeration(); }
                });
                modernServlet.delegate = servlet;
                initialized.set(true);
                return;
            }
            Object provider = getTransportProvider();
            if (provider instanceof io.modelcontextprotocol.spec.McpStatelessServerTransport stateless) {
                initializeStateless(stateless);
                delegate();
                initialized.set(true);
                return;
            }
            McpServer.SyncSpecification<?> spec = provider instanceof io.modelcontextprotocol.spec.McpStreamableServerTransportProvider streamable
                    ? McpServer.sync(streamable)
                    : McpServer.sync((McpServerTransportProvider) provider);
            synchronized (pendingFeatures) {
                spec.serverInfo(configuration.getServerName(), configuration.getServerVersion());
                spec.capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(false, true)
                        .prompts(true)
                        .completions()
                        .logging()
                        .build());
                spec.resources(pendingFeatures.stream().flatMap(f -> f.resources().stream()).toList());
                spec.prompts(pendingFeatures.stream().flatMap(f -> f.prompts().stream()).toList());
                spec.completions(pendingFeatures.stream().flatMap(f -> f.completions().stream()).toList());
                mcpSyncServer = spec instanceof McpServer.StreamableSyncSpecification st ? st.build()
                        : ((McpServer.SingleSessionSyncSpecification) spec).build();
                pendingFeatures.forEach(f -> f.resourceTemplates().forEach(mcpSyncServer::addResourceTemplate));
                pendingFeatures.clear();
            }

            delegate();
            initialized.set(true);
            log.debug("MCP Mediator initialized successfully");
        } catch (Exception e) {
            log.info("stopping the MCP Mediator server");
            closeServer();
            log.info("MCP Mediator server stopped");
            throw new McpMediatorException("Failed to initialize MCP Mediator", e);
        }
    }

    private void initializeStateless(io.modelcontextprotocol.spec.McpStatelessServerTransport transport) {
        synchronized (pendingFeatures) {
            var spec = McpServer.sync(transport)
                    .serverInfo(configuration.getServerName(), configuration.getServerVersion())
                    .capabilities(McpSchema.ServerCapabilities.builder()
                            .tools(true).resources(false, true).prompts(true).completions().build());
            spec.resources(pendingFeatures.stream().flatMap(f -> f.resources().stream())
                    .map(DefaultMcpMediator::statelessResource).toList());
            spec.prompts(pendingFeatures.stream().flatMap(f -> f.prompts().stream())
                    .map(DefaultMcpMediator::statelessPrompt).toList());
            spec.completions(pendingFeatures.stream().flatMap(f -> f.completions().stream())
                    .map(DefaultMcpMediator::statelessCompletion).toList());
            statelessServer = spec.build();
            pendingFeatures.forEach(f -> f.resourceTemplates()
                    .forEach(t -> statelessServer.addResourceTemplate(statelessTemplate(t))));
            pendingFeatures.clear();
        }
    }

    private synchronized void closeServer() {
        if (statelessServer != null) {
            statelessServer.closeGracefully();
            statelessServer = null;
        }
        if (mcpSyncServer != null) {
            mcpSyncServer.closeGracefully();
            mcpSyncServer = null;
        }
    }

    @NonNull
    @Override
    @SuppressWarnings("rawtypes")
    public List<McpMediatorRequestHandler> getHandlers() {
        return Collections.unmodifiableList(handlersList);
    }

    /**
     * Executes a given {@link McpMediatorRequest} using its corresponding {@link McpMediatorRequestHandler}.
     * <p>
     * The method handles the full lifecycle of request execution, including:
     * <ul>
     *   <li>Retrieving the parent {@link McpExecutionContext}</li>
     *   <li>Finding and validating the appropriate handler</li>
     *   <li>Wrapping the execution inside a {@link McpRequestExecutor} to maintain execution context isolation</li>
     *   <li>Submitting the execution to an {@link java.util.concurrent.ExecutorService}</li>
     *   <li>Handling and propagating execution errors, wrapping them into a {@link McpMediatorException}</li>
     * </ul>
     *
     * <p>Important details:
     * <ul>
     *   <li>If the execution fails, a detailed error will be logged and wrapped into a {@link McpMediatorException}.</li>
     *   <li>If the execution thread is interrupted, it re-interrupts the current thread and throws a {@link McpMediatorException}.</li>
     * </ul>
     *
     * @param request the request to be executed
     * @param <T>     the type of the mediator request
     * @param <R>     the type of the response expected from the handler
     * @return the result produced by the handler
     * @throws McpMediatorException if any error occurs during handler validation, execution, or interruption.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends McpMediatorRequest<R>, R> R execute(T request) throws McpMediatorException {
        final McpExecutionContext parentContext = McpExecutionContext.get();
        final McpCallContext callContext = McpCallContext.get();
        final McpMediatorRequestHandler<T, R> handler = (McpMediatorRequestHandler<T, R>) findHandler(request);

        McpRequestExecutor<R> executor = new McpRequestExecutor<>() {
            @Override
            public R call() {
                Tracer tracer = configuration.getOpenTelemetry().getTracer("mcp-mediator-core");
                Span span = tracer.spanBuilder("mcp_handler_execute").startSpan();
                McpExecutionContext.set(MinimalMcpMediator.of(DefaultMcpMediator.this),
                        configuration.getSerializer(), parentContext);
                McpCallContext.set(callContext);

                validateHandler(handler, request);
                try (io.opentelemetry.context.Scope scope = span.makeCurrent()) {
                    span.setAttribute("mcp.request.type", request.getClass().getSimpleName());
                    R result = handler.handle(request);
                    span.setStatus(StatusCode.OK);
                    return result;
                } catch (Exception e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR, e.getMessage());
                    log.error("Failed to execute request {}", request, e);
                    throw new McpMediatorException(e.getMessage(), e);
                } finally {
                    McpExecutionContext.remove();
                    McpCallContext.set(null);
                    span.end();
                }
            }
        };

        Future<R> executionSyncedResult = executorService.submit(executor);
        try {
            return executionSyncedResult.get();
        } catch (ExecutionException e) {
            throw new McpMediatorException(String.format("Failed to execute request [%s]: %s", request, e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = String.format("Internal handler execution interrupted! interrupting mediator! request: %s", request);
            log.error(message, e);
            throw new McpMediatorException(message, e);
        }
    }

    private static <T extends McpMediatorRequest<R>, R> void validateHandler(McpMediatorRequestHandler<T, R> handler, T request) {
        if (handler == null) {
            throw new McpMediatorException(String.format("No handler found for request type %s", request));
        }
    }

    protected void delegate() {
        handlersList.forEach(this::addToHandlersMap);
        handlersMap.forEach((requestType, handler) ->
                startHandlerToMcpToolConnection(requestType, handler, false));
    }

    private void startHandlerToMcpToolConnection(Class<? extends McpMediatorRequest<?>> requestType,
                                                 McpMediatorRequestHandler<?, ?> handler, boolean notifyClients) {
        Collection<? extends McpToolAdapter<?>> adapters = McpAdapterFactory.createAdapter(requestType, handler);
        adapters.forEach(adapter -> {
            McpServerFeatures.SyncToolSpecification tool =
                    createMcpToolSpecification(adapter, clientPassedArgs ->
                            executeClientCall(clientPassedArgs, requestType));
            serverAddTool(tool);
        });

        if (notifyClients && mcpSyncServer != null) {
            mcpSyncServer.notifyToolsListChanged();
            log.debug("All tools registered successfully");
        }
    }

    private <T extends McpMediatorRequest<R>, R> void addToHandlersMap(@NonNull McpMediatorRequestHandler<T, R> handler) {
        handler.initialize(configuration.getSerializer());
        handler.getAllSupportedRequestClass().forEach(reqClass -> handlersMap.put(reqClass, handler));
    }

    @NonNull
    protected McpServerFeatures.SyncToolSpecification createMcpToolSpecification(
            @NonNull McpToolAdapter<?> adapter,
            @NonNull Function<McpSchema.CallToolRequest, McpSchema.CallToolResult> functionToCall) {

        return new McpServerFeatures.SyncToolSpecification(defineMcpTool(adapter),
                (mcpSyncServerExchange, callToolRequest) -> {
                    Tracer tracer = configuration.getOpenTelemetry().getTracer("mcp-mediator-core");
                    Span span = tracer.spanBuilder("mcp_tool_execution").startSpan();
                    McpCallContext.set(McpCallContext.of(mcpSyncServerExchange, callToolRequest));
                    if (adapter.getSourceTool() instanceof java.lang.reflect.Method toolMethod) {
                        try {
                            McpAnnotations.find(toolMethod, "McpTool")
                                    .ifPresent(a -> McpFeatureScanner.requireCapabilities(a, mcpSyncServerExchange));
                        } catch (McpMediatorException missing) {
                            McpCallContext.set(null);
                            span.end();
                            return McpSchema.CallToolResult.builder()
                                    .content(List.of(new McpSchema.TextContent(missing.getMessage()))).isError(true).build();
                        }
                    }
                    try (io.opentelemetry.context.Scope scope = span.makeCurrent()) {
                        span.setAttribute("mcp.tool.name", callToolRequest.name());
                        McpSchema.CallToolResult result = functionToCall.apply(callToolRequest);
                        if (result != null && result.isError() != null && result.isError()) {
                            span.setStatus(StatusCode.ERROR, "Tool execution returned error result");
                        } else {
                            span.setStatus(StatusCode.OK);
                        }
                        return result;
                    } catch (Exception e) {
                        log.error("Failed to execute the request, sending error to client", e);
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR, e.getMessage());
                        String errorMessage = e.getMessage();
                        if (configuration.getExceptionHandler() != null) {
                            try {
                                errorMessage = configuration.getExceptionHandler().handleException(null, e);
                            } catch (Exception ex) {
                                log.error("Exception handler failed", ex);
                            }
                        }
                        if (mcpSyncServerExchange != null) mcpSyncServerExchange.loggingNotification(new McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.DEBUG, e.getMessage(), e.getStackTrace().toString()));
                        return McpSchema.CallToolResult.builder().content(List.of(new McpSchema.TextContent(errorMessage))).isError(true).build();
                    } finally {
                        McpCallContext.set(null);
                        span.end();
                    }
                });
    }

    private synchronized Object getTransportProvider() {
        if (transportProvider == null) {
            McpJsonMapper json = new JacksonMcpJsonMapper(configuration.getSerializer());
            transportProvider = switch (configuration.getTransportType()) {
                case STDIO -> new StdioServerTransportProvider(json,
                        configuration.getStdioInputStream(), configuration.getStdioOutputStream());
                case SSE -> HttpServletSseServerTransportProvider.builder()
                        .jsonMapper(json).baseUrl(configuration.getServerAddress() == null ? "" : configuration.getServerAddress())
                        .sseEndpoint("/sse").messageEndpoint("/mcp/message").build();
                case MODERN_HTTP -> new Object(); // served by McpModernServer, see initialize()
                case STATELESS_HTTP -> io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport
                        .builder().jsonMapper(json).messageEndpoint("/mcp").build();
                case STREAMABLE_HTTP -> io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider
                        .builder().jsonMapper(json).mcpEndpoint("/mcp").build();
            };
        }
        return transportProvider;
    }

    /**
     * The servlet to mount when the transport is {@code SSE} or {@code STREAMABLE_HTTP} (streamable endpoint: {@code /mcp}).
     *
     * @throws McpMediatorException for the STDIO transport
     */
    public jakarta.servlet.http.HttpServlet getHttpServlet() {
        if (configuration.getTransportType() == io.github.makbn.mcp.mediator.api.McpTransportType.MODERN_HTTP) {
            return modernServlet;
        }
        if (getTransportProvider() instanceof jakarta.servlet.http.HttpServlet servlet) {
            return servlet;
        }
        throw new McpMediatorException("Transport " + configuration.getTransportType() + " is not HTTP based");
    }

    /**
     * Finds a handler that can process the given request.
     *
     * @param request the request to find a handler for
     * @return the handler that can process the request or null if none found
     */
    @SuppressWarnings("unchecked")
    private McpMediatorRequestHandler<?, ?> findHandler(@NonNull McpMediatorRequest<?> request) {
        return handlersMap.values().stream().filter(handler
                -> handler.canHandle(request)).findFirst().orElse(null);
    }

    @NonNull
    private McpSchema.Tool defineMcpTool(@NonNull McpToolAdapter<?> adapter) {
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(configuration.getSerializer());
        McpSchema.Tool.Builder builder = McpSchema.Tool.builder(adapter.getMethod(), jsonMapper, adapter.getSchema())
                .description(adapter.getDescription());
        if (adapter.getSourceTool() instanceof java.lang.reflect.Method method) {
            McpAnnotations.find(method, "McpTool").ifPresent(a -> {
                String title = McpAnnotations.str(a, "title", null);
                McpAnnotations.nested(a, "annotations").ifPresent(h -> builder.annotations(
                        new McpSchema.ToolAnnotations(McpAnnotations.str(h, "title", title),
                                McpAnnotations.bool(h, "readOnlyHint", false),
                                McpAnnotations.bool(h, "destructiveHint", false),
                                McpAnnotations.bool(h, "idempotentHint", false),
                                McpAnnotations.bool(h, "openWorldHint", false), null)));
                builder.title(title);
                builder.icons(McpFeatureScanner.icons(a));
                builder.meta(McpAnnotations.meta(a));
                if (McpAnnotations.bool(a, "generateOutputSchema", false)) {
                    String out = McpMethodSchemaGenerator.of(configuration.getSerializer())
                            .generateOutputSchemaForMethod(method);
                    if (out != null) {
                        builder.outputSchema(jsonMapper, out);
                    }
                }
            });
        }
        return builder.build();
    }

    private McpSchema.CallToolResult executeClientCall(
            McpSchema.CallToolRequest mcpClientRequest,
            Class<? extends McpMediatorRequest<?>> mcpMediatorRequestType) {
        Tracer tracer = configuration.getOpenTelemetry().getTracer("mcp-mediator-core");
        Span span = tracer.spanBuilder("mcp_tool_call").startSpan();
        McpMediatorRequest<?> mcpMediatorRequest = null;
        try (io.opentelemetry.context.Scope scope = span.makeCurrent()) {
            span.setAttribute("mcp.tool.name", mcpClientRequest.name());
            if (configuration.getInterceptors() != null) {
                for (McpMediatorInterceptor interceptor : configuration.getInterceptors()) {
                    interceptor.intercept(mcpClientRequest);
                }
            }
            mcpMediatorRequest = configuration.getSerializer()
                    .convertValue(mcpClientRequest.arguments(), mcpMediatorRequestType);
            Object mcpMediatorResult = execute(mcpMediatorRequest);
            span.setStatus(StatusCode.OK);
            McpSchema.CallToolResult.Builder ok = McpSchema.CallToolResult.builder()
                    .content(java.util.List.of(new McpSchema.TextContent(serialize(mcpMediatorResult))))
                    .isError(false);
            if (mcpMediatorResult != null && !(mcpMediatorResult instanceof CharSequence)
                    && !(mcpMediatorResult instanceof Number) && !(mcpMediatorResult instanceof Boolean)
                    && !(mcpMediatorResult instanceof java.util.Collection) && !mcpMediatorResult.getClass().isArray()
                    && !mcpMediatorResult.getClass().isEnum()) {
                ok.structuredContent(configuration.getSerializer().convertValue(mcpMediatorResult, java.util.Map.class));
            }
            return ok.build();
        } catch (Exception e) {
            log.error("Failed to execute tool call", e);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            String errorMessage = e.getMessage();
            if (configuration.getExceptionHandler() != null) {
                try {
                    errorMessage = configuration.getExceptionHandler().handleException(mcpMediatorRequest, e);
                } catch (Exception ex) {
                    log.error("Exception handler failed", ex);
                }
            }
            return McpSchema.CallToolResult.builder()
                    .content(java.util.List.of(new McpSchema.TextContent(errorMessage)))
                    .isError(true)
                    .build();
        } finally {
            span.end();
        }
    }

    @NonNull
    private String serialize(@NonNull Object object) throws JsonProcessingException {
        return configuration.getSerializer().writeValueAsString(object);
    }
}