package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.api.McpMediatorConfig;
import io.github.makbn.mcp.mediator.api.McpMediatorException;

/**
 * Generic implementation of the MCP Mediator.
 * This implementation delegates all operations to registered handlers and provides
 * a simple lifecycle management.
 */
public class GenericMcpMediator extends AbstractMcpMediator {

    @Override
    protected void doInitialize() throws McpMediatorException {
        // No specific initialization needed
    }

    @Override
    protected void doStart() throws McpMediatorException {
        // No specific start operations needed
    }

    @Override
    protected void doStop() throws McpMediatorException {
        // No specific stop operations needed
    }
} 