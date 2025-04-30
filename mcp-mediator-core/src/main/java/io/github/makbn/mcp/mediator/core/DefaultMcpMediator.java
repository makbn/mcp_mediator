package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.makbn.mcp.mediator.api.*;
import io.github.makbn.mcp.mediator.core.adaper.McpAdapterFactory;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorDefaultConfiguration;
import io.github.makbn.mcp.mediator.core.internal.McpRequestExecutor;
import io.github.makbn.mcp.mediator.core.internal.MinimalMcpMediator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import lombok.AccessLevel;
import lombok.NonNull;
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

    @NonFinal
    ExecutorService executorService;
    @NonFinal
    McpSyncServer mcpSyncServer;

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
            McpServerTransportProvider stdioServerTransportProvider = getMcpServerTransportProvider();
            mcpSyncServer = McpServer.sync(stdioServerTransportProvider)
                    .serverInfo(configuration.getServerName(), configuration.getServerVersion())
                    .capabilities(McpSchema.ServerCapabilities.builder()
                            .tools(true)
                            .build())
                    .build();

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

    private synchronized void closeServer() {
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
        final McpMediatorRequestHandler<T, R> handler = (McpMediatorRequestHandler<T, R>) findHandler(request);

        McpRequestExecutor<R> executor = new McpRequestExecutor<>() {
            @Override
            public R call() {
                McpExecutionContext.set(MinimalMcpMediator.of(DefaultMcpMediator.this),
                        configuration.getSerializer(), parentContext);

                validateHandler(handler, request);
                try {
                    return handler.handle(request);
                } catch (Exception e) {
                    log.error("Failed to execute request {}", request, e);
                    throw new McpMediatorException(String.format("Failed to execute request %s", request), e);
                } finally {
                    McpExecutionContext.remove();
                }
            }
        };

        Future<R> executionSyncedResult = executorService.submit(executor);
        try {
            return executionSyncedResult.get();
        } catch (ExecutionException e) {
            throw new McpMediatorException(String.format("Failed to execute request %s", request), e);
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
            mcpSyncServer.addTool(tool);
        });

        if (notifyClients) {
            mcpSyncServer.notifyToolsListChanged();
            log.debug("all tools registered successfully {}", mcpSyncServer);
        }
    }

    private <T extends McpMediatorRequest<R>, R> void addToHandlersMap(@NonNull McpMediatorRequestHandler<T, R> handler) {
        handler.initialize(configuration.getSerializer());
        handler.getAllSupportedRequestClass().forEach(reqClass -> handlersMap.put(reqClass, handler));
    }

    @NonNull
    protected McpServerFeatures.SyncToolSpecification createMcpToolSpecification(
            @NonNull McpToolAdapter<?> adapter,
            @NonNull Function<Map<String, Object>, McpSchema.CallToolResult> functionToCall) {

        return new McpServerFeatures.SyncToolSpecification(defineMcpTool(adapter),
                (mcpSyncServerExchange, stringObjectMap) -> {
                    try {
                        return functionToCall.apply(stringObjectMap);
                    } catch (Exception e) {
                        log.error("Failed to execute the request, sending error to client", e);
                        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(e.getMessage())), true);
                    }
                });
    }

    private McpServerTransportProvider getMcpServerTransportProvider() {
        return switch (configuration.getTransportType()) {
            case STDIO -> new StdioServerTransportProvider(configuration.getSerializer(),
                    configuration.getStdioInputStream(),
                    configuration.getStdioOutputStream());
            case SSE -> new HttpServletSseServerTransportProvider(configuration.getSerializer(),
                    configuration.getServerAddress());
        };
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
        return new McpSchema.Tool(adapter.getMethod(), adapter.getDescription(), adapter.getSchema());
    }

    private McpSchema.CallToolResult executeClientCall(
            Map<String, Object> mcpClientRequestParameters,
            Class<? extends McpMediatorRequest<?>> mcpMediatorRequestType) {
        try {
            McpMediatorRequest<?> mcpMediatorRequest = configuration.getSerializer()
                    .convertValue(mcpClientRequestParameters, mcpMediatorRequestType);
            Object mcpMediatorResult = execute(mcpMediatorRequest);

            return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(serialize(mcpMediatorResult))), false);
        } catch (IOException e) {
            throw new McpMediatorException("Failed to execute request", e);
        }
    }

    @NonNull
    private String serialize(@NonNull Object object) throws JsonProcessingException {
        return configuration.getSerializer().writeValueAsString(object);
    }
}