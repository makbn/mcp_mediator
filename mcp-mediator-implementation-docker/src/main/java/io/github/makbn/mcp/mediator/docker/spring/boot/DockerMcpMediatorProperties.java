package io.github.makbn.mcp.mediator.docker.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Docker MCP Mediator in Spring Boot applications.
 */
@ConfigurationProperties(prefix = "mcp.mediator.docker")
public class DockerMcpMediatorProperties {
    private String host = "unix:///var/run/docker.sock";
    private String certPath;
    private String configPath;
    private String apiVersion = "1.41";

    /**
     * Gets the Docker host URI.
     *
     * @return the Docker host URI
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the Docker host URI.
     *
     * @param host the Docker host URI
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets the Docker certificate path.
     *
     * @return the Docker certificate path
     */
    public String getCertPath() {
        return certPath;
    }

    /**
     * Sets the Docker certificate path.
     *
     * @param certPath the Docker certificate path
     */
    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    /**
     * Gets the Docker config path.
     *
     * @return the Docker config path
     */
    public String getConfigPath() {
        return configPath;
    }

    /**
     * Sets the Docker config path.
     *
     * @param configPath the Docker config path
     */
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    /**
     * Gets the Docker API version.
     *
     * @return the Docker API version
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Sets the Docker API version.
     *
     * @param apiVersion the Docker API version
     */
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
} 