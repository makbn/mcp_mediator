package io.github.makbn.mcp.mediator.docker;

import io.github.makbn.mcp.mediator.api.McpMediatorConfig;
import io.github.makbn.mcp.mediator.api.McpMediatorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Docker-specific configuration for MCP Mediator.
 */
public class DockerMcpMediatorConfig implements McpMediatorConfig {
    private static final String DOCKER_HOST = "docker.host";
    private static final String DOCKER_CERT_PATH = "docker.cert.path";
    private static final String DOCKER_CONFIG_PATH = "docker.config.path";
    private static final String DOCKER_API_VERSION = "docker.api.version";

    private final Map<String, String> properties;

    public DockerMcpMediatorConfig(Map<String, String> properties) {
        this.properties = new HashMap<>(properties);
    }

    @Override
    public String getImplementationName() {
        return "docker";
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>(properties);
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    @Override
    public void validate() throws McpMediatorException {
        // Docker host is required
        if (getProperty(DOCKER_HOST) == null) {
            throw new McpMediatorException("Docker host must be specified", null);
        }
    }

    /**
     * Gets the Docker host URI.
     *
     * @return the Docker host URI
     */
    public String getDockerHost() {
        return getProperty(DOCKER_HOST);
    }

    /**
     * Gets the Docker certificate path.
     *
     * @return the Docker certificate path, or null if not specified
     */
    public String getDockerCertPath() {
        return getProperty(DOCKER_CERT_PATH);
    }

    /**
     * Gets the Docker config path.
     *
     * @return the Docker config path, or null if not specified
     */
    public String getDockerConfigPath() {
        return getProperty(DOCKER_CONFIG_PATH);
    }

    /**
     * Gets the Docker API version.
     *
     * @return the Docker API version, or null if not specified
     */
    public String getDockerApiVersion() {
        return getProperty(DOCKER_API_VERSION);
    }
} 