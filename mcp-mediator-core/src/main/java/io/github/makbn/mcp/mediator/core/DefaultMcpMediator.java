package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.*;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default base implementation of the MCP Mediator for MCP Stdio.
 * Provides common functionality for request handling and state management.
 *
 * @author Matt Akbarian
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultMcpMediator implements McpMediator {
    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends McpMediatorRequest<?>>, McpMediatorRequestHandler> handlers = new ConcurrentHashMap<>();

    McpMediatorConfigurationSpec configuration;

    @NonFinal
    McpSyncServer mcpSyncServer;

    public DefaultMcpMediator() {
        this(McpMediatorConfiguration.builder().withDefaults().build());
    }

    public DefaultMcpMediator(@NonNull McpMediatorConfigurationSpec configuration) {
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
            StdioServerTransportProvider stdioServerTransportProvider = new StdioServerTransportProvider();
            mcpSyncServer = McpServer.sync(stdioServerTransportProvider)
                    .serverInfo(configuration.getServerName(), configuration.getServerVersion())
                    .capabilities(McpSchema.ServerCapabilities.builder()
                            .tools(configuration.isToolsEnabled())
                            .build())
                    .build();

            handlers.forEach((mcpMediatorRequestType, handler) -> {
                McpRequestAdapter adapter = McpRequestAdapter.builder().request(mcpMediatorRequestType).build();
                mcpSyncServer.addTool(new McpServerFeatures.SyncToolSpecification(defineMcpTool(adapter),
                        (mcpSyncServerExchange, stringObjectMap) -> {
                            try {
                                return new McpSchema.CallToolResult(execute(stringObjectMap, mcpMediatorRequestType), false);
                            } catch (Exception e) {
                                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(e.getMessage())), true);
                            }
                        }));
            });
            log.debug("MCP Mediator initialized successfully");
        } catch (Exception e) {
            mcpSyncServer.closeGracefully();
            mcpSyncServer = null;
            throw new McpMediatorException("Failed to initialize MCP Mediator", e);
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

    /**
     * Finds a handler that can process the given request.
     *
     * @param request the request to find a handler for
     * @return the handler that can process the request, or null if none found
     */
    @SuppressWarnings("unchecked")
    private McpMediatorRequestHandler<?, ?> findHandler(@NonNull McpMediatorRequest<?> request) {
        return handlers.values().stream().filter(handler
                -> handler.canHandle(request)).findFirst().orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends McpMediatorRequest<R>, R> R execute(T request) throws McpMediatorException {
        McpMediatorRequestHandler<T, R> handler = (McpMediatorRequestHandler<T, R>) findHandler(request);
        if (handler == null) {
            throw new McpMediatorException(String.format("No handler found for request type %s", request));
        }

        try {
            return handler.handle(request);
        } catch (Exception e) {
            throw new McpMediatorException(String.format("Failed to execute request %s", request), e);
        }
    }

    @NonNull
    private McpSchema.Tool defineMcpTool(@NonNull McpRequestAdapter adapter) {
        return new McpSchema.Tool(adapter.getMethod(), adapter.getDescription(), adapter.getSchema());
    }

    @NonNull
    private <T extends McpMediatorRequest<R>, R> List<McpSchema.Content> execute(
            @NonNull Map<String, Object> mcpClientRequestParameters, @NonNull Class<T> mcpMediatorRequestType) {

        R mcpMediatorResult;
        try {
            T mcpMediatorRequest = new ObjectMapper().convertValue(mcpClientRequestParameters, mcpMediatorRequestType);
            mcpMediatorResult = execute(mcpMediatorRequest);
            return List.of(new McpSchema.TextContent(serialize(mcpMediatorResult)));
        } catch (IOException e) {
            throw new McpMediatorException("Failed to execute request", e);
        }
    }

    @NonNull
    private String serialize(@NonNull Object object) throws JsonProcessingException {
        return ((ObjectMapper) configuration.getSerializer()).writeValueAsString(object);
    }
} 