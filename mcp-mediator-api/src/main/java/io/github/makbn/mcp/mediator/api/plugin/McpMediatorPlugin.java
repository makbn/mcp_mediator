package io.github.makbn.mcp.mediator.api.plugin;

import org.slf4j.event.Level;

import java.util.List;

/**
 * Specification class for plugins. Plugins are utility features that can be plugged into the mediator to
 * add a feature, observe the current state, manage the mediator, etc.
 */
public interface McpMediatorPlugin {

    String name();

    List<McpMediatorResourceSpec<?>> acquiredResources();

    void initialize();

    int shutdown();

    void log(Level level, String message, Object... args);
}
