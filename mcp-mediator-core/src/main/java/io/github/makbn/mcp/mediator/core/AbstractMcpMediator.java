package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpMediatorConfig;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorListener;
import io.github.makbn.mcp.mediator.api.McpMediatorStatus;
import io.github.makbn.mcp.mediator.api.McpRequest;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base implementation of the MCP Mediator.
 * Provides common functionality for request handling and state management.
 */
public abstract class AbstractMcpMediator implements McpMediator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMcpMediator.class);

    private McpMediatorConfig config;
    private McpMediatorStatus status = McpMediatorStatus.CREATED;
    private final List<McpMediatorListener> listeners = new CopyOnWriteArrayList<>();
    private final List<McpRequestHandler<?>> handlers = new CopyOnWriteArrayList<>();

    @Override
    public void initialize(McpMediatorConfig config) throws McpMediatorException {
        LOG.info("Initializing MCP Mediator");
        setStatus(McpMediatorStatus.INITIALIZING);
        
        try {
            this.config = config;
            config.validate();
            doInitialize();
            setStatus(McpMediatorStatus.INITIALIZED);
            LOG.info("MCP Mediator initialized successfully");
        } catch (Exception e) {
            setStatus(McpMediatorStatus.ERROR);
            throw new McpMediatorException("Failed to initialize MCP Mediator", e, status);
        }
    }

    @Override
    public void start() throws McpMediatorException {
        LOG.info("Starting MCP Mediator");
        if (status != McpMediatorStatus.INITIALIZED) {
            throw new McpMediatorException("Mediator must be initialized before starting", status);
        }

        setStatus(McpMediatorStatus.STARTING);
        try {
            doStart();
            setStatus(McpMediatorStatus.RUNNING);
            LOG.info("MCP Mediator started successfully");
        } catch (Exception e) {
            setStatus(McpMediatorStatus.ERROR);
            throw new McpMediatorException("Failed to start MCP Mediator", e, status);
        }
    }

    @Override
    public void stop() throws McpMediatorException {
        LOG.info("Stopping MCP Mediator");
        if (status != McpMediatorStatus.RUNNING) {
            throw new McpMediatorException("Mediator must be running before stopping", status);
        }

        setStatus(McpMediatorStatus.STOPPING);
        try {
            doStop();
            setStatus(McpMediatorStatus.STOPPED);
            LOG.info("MCP Mediator stopped successfully");
        } catch (Exception e) {
            setStatus(McpMediatorStatus.ERROR);
            throw new McpMediatorException("Failed to stop MCP Mediator", e, status);
        }
    }

    @Override
    public boolean isRunning() {
        return status == McpMediatorStatus.RUNNING;
    }

    @Override
    public McpMediatorStatus getStatus() {
        return status;
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
    public void registerHandler(McpRequestHandler<?> handler) {
        handlers.add(handler);
        LOG.info("Registered handler for {} requests in {} implementation", 
            handler.getRequestType(), handler.getImplementationName());
    }

    @Override
    public void unregisterHandler(McpRequestHandler<?> handler) {
        handlers.remove(handler);
        LOG.info("Unregistered handler for {} requests in {} implementation", 
            handler.getRequestType(), handler.getImplementationName());
    }

    @Override
    public List<McpRequestHandler<?>> getHandlers() {
        return new ArrayList<>(handlers);
    }

    @Override
    public Object execute(McpRequest request) throws McpMediatorException {
        if (!isRunning()) {
            throw new McpMediatorException("Mediator is not running", status);
        }

        LOG.debug("Executing request of type {} for implementation {}", 
            request.getType(), request.getImplementationName());

        McpRequestHandler<?> handler = findHandler(request);
        if (handler == null) {
            throw new McpMediatorException(
                String.format("No handler found for request type %s in implementation %s", 
                    request.getType(), request.getImplementationName()),
                status);
        }

        try {
            return handler.handle(request);
        } catch (Exception e) {
            throw new McpMediatorException(
                String.format("Failed to execute request of type %s", request.getType()),
                e, status);
        }
    }

    /**
     * Finds a handler that can process the given request.
     *
     * @param request the request to find a handler for
     * @return the handler that can process the request, or null if none found
     */
    protected McpRequestHandler<?> findHandler(McpRequest request) {
        return handlers.stream()
            .filter(handler -> handler.canHandle(request))
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets the current configuration.
     *
     * @return the current configuration
     */
    protected McpMediatorConfig getConfig() {
        return config;
    }

    /**
     * Sets the current status and notifies listeners.
     *
     * @param newStatus the new status
     */
    private void setStatus(McpMediatorStatus newStatus) {
        McpMediatorStatus oldStatus = this.status;
        this.status = newStatus;
        notifyStatusChange(oldStatus, newStatus);
    }

    /**
     * Notifies all registered listeners of a status change.
     *
     * @param oldStatus the previous status
     * @param newStatus the new status
     */
    protected void notifyStatusChange(McpMediatorStatus oldStatus, McpMediatorStatus newStatus) {
        for (McpMediatorListener listener : listeners) {
            try {
                listener.onStatusChange(oldStatus, newStatus);
            } catch (Exception e) {
                LOG.error("Error notifying listener of status change", e);
            }
        }
    }

    /**
     * Performs implementation-specific initialization.
     *
     * @throws McpMediatorException if initialization fails
     */
    protected abstract void doInitialize() throws McpMediatorException;

    /**
     * Performs implementation-specific start operations.
     *
     * @throws McpMediatorException if starting fails
     */
    protected abstract void doStart() throws McpMediatorException;

    /**
     * Performs implementation-specific stop operations.
     *
     * @throws McpMediatorException if stopping fails
     */
    protected abstract void doStop() throws McpMediatorException;
} 