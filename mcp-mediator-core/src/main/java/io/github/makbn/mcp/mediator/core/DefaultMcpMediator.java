package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.makbn.mcp.mediator.api.*;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
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
    Map<Class<? extends McpMediatorRequest<?>>, McpMediatorRequestHandler> handlers = new ConcurrentHashMap<>();
    McpMediatorDefaultConfiguration configuration;
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
     * Initializes the mediator, creating the internal MCP server and registering all known tools.
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

    /**
     * Registers a request handler capable of handling one or more MCP request types.
     *
     * @param handler the request handler to register
     * @param <T>     the request type
     * @param <R>     the result type
     */
    @Override
    public <T extends McpMediatorRequest<R>, R> void registerHandler(@NonNull McpMediatorRequestHandler<T, R> handler) {
        handler.getAllSupportedRequestClass().forEach(reqClass -> handlers.put(reqClass, handler));
    }

    @NonNull
    @Override
    @SuppressWarnings("rawtypes")
    public List<McpMediatorRequestHandler> getHandlers() {
        return handlers.values().stream().toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends McpMediatorRequest<R>, R> R execute(T request) throws McpMediatorException {
        final McpExecutionContext parentContext = McpExecutionContext.get();
        final McpMediatorRequestHandler<T, R> handler = (McpMediatorRequestHandler<T, R>) findHandler(request);

        McpRequestExecutor<R> executor = new McpRequestExecutor<>() {
            @Override
            public R call() {
                McpExecutionContext.set(MinimalMcpMediator.of(DefaultMcpMediator.this), parentContext);
                if (handler == null) {
                    throw new McpMediatorException(String.format("No handler found for request type %s", request));
                }
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
        }catch (ExecutionException e){
            throw new McpMediatorException(String.format("Failed to execute request %s", request), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = String.format("Internal handler execution interrupted! interrupting mediator! request: %s", request);
            log.error(message, e);
            throw new McpMediatorException(message, e);
        }
    }

    protected void delegate() {
        handlers.forEach((mediatorRequestType, handler) -> {
            McpRequestAdapter adapter = McpRequestAdapter.builder().request(mediatorRequestType).build();
            mcpSyncServer.addTool(createMcpToolSpecification(adapter,
                    clientPassedArgs -> executeClientCall(clientPassedArgs, mediatorRequestType)));
        });
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
        return handlers.values().stream().filter(handler
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