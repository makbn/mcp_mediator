package io.github.makbn.mcp.mediator.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for MCP Mediator in Spring Boot applications.
 */
@ConfigurationProperties(prefix = "mcp.mediator")
public class McpMediatorProperties {
    /** Enables the auto-configured mediator that exposes annotated beans. Off by default (stdio owns stdin/stdout). */
    private boolean enabled;
    /** STDIO (default), SSE or STREAMABLE_HTTP. HTTP transports are mounted as a servlet at {@code /mcp}. */
    private io.github.makbn.mcp.mediator.api.McpTransportType transport = io.github.makbn.mcp.mediator.api.McpTransportType.STDIO;
    private String serverName = "mcp_mediator_server";
    private String serverVersion = "1.0.0";
    private String implementationName;
    private Map<String, String> properties = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public io.github.makbn.mcp.mediator.api.McpTransportType getTransport() {
        return transport;
    }

    public void setTransport(io.github.makbn.mcp.mediator.api.McpTransportType transport) {
        this.transport = transport;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    /**
     * Gets the implementation name.
     *
     * @return the implementation name
     */
    public String getImplementationName() {
        return implementationName;
    }

    /**
     * Sets the implementation name.
     *
     * @param implementationName the implementation name
     */
    public void setImplementationName(String implementationName) {
        this.implementationName = implementationName;
    }

    /**
     * Gets the properties map.
     *
     * @return the properties map
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Sets the properties map.
     *
     * @param properties the properties map
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
} 