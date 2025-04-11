package io.github.makbn.mcp.mediator.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for MCP Mediator in Spring Boot applications.
 */
@ConfigurationProperties(prefix = "mcp.mediator")
public class McpMediatorProperties {
    private String implementationName;
    private Map<String, String> properties = new HashMap<>();

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