package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base implementation of the MCP Mediator.
 * Provides common functionality for request handling and state management.
 */
public class DefaultMcpMediator implements io.github.makbn.mcp.mediator.api.McpMediator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMcpMediator.class);


    private final List<McpMediatorListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends McpRequest<?>>, McpRequestHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    @Override
    public void initialize() throws McpMediatorException {
        LOG.info("Initializing MCP Mediator");
        try {

            LOG.debug("MCP Mediator initialized successfully");
        } catch (Exception e) {
            throw new McpMediatorException("Failed to initialize MCP Mediator", e);
        }
    }

    @Override
    public void registerListener(McpMediatorListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(McpMediatorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void registerHandler(McpRequestHandler<?, ?> handler) {
        handlers.put(handler.getRequestClass(), handler);
    }

    @Override
    public void unregisterHandler(McpRequestHandler<?, ?> handler) {
        handlers.remove(handler);
        LOG.info("Unregistered handler for {} requests",  handler.getName());
    }


    /**
     * Finds a handler that can process the given request.
     *
     * @param request the request to find a handler for
     * @return the handler that can process the request, or null if none found
     */
    protected McpRequestHandler<?, ?> findHandler(McpRequest<?> request) {
        return handlers.values()
                .stream()
            .filter(handler -> handler.canHandle(request))
            .findFirst()
            .orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends McpRequest<R>, R> R execute(T request) throws McpMediatorException {
        McpRequestHandler<T, R> handler = (McpRequestHandler<T, R>) findHandler(request);
        if (handler == null) {
            throw new McpMediatorException(String.format("No handler found for request type %s", request.getMethod()));
        }

        try {
            return handler.handle(request);
        } catch (Exception e) {
            throw new McpMediatorException(String.format("Failed to execute request %s", request.getMethod()), e);
        }
    }
} 