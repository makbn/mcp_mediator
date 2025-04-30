package io.github.makbn.mcp.mediator.docker.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.github.makbn.mcp.mediator.core.DefaultMcpMediator;
import io.github.makbn.mcp.mediator.core.McpServiceFactory;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.docker.internal.DockerClientService;

public class DockerMcpServer {
    private static final String DOCKER_MCP_SERVER = "docker_mcp_server";
    private static final String DOCKER_MCP_SERVER_VER = "1.0.0.0";

    public static void main(String[] args) {
        DockerClient dockerClient = intitializeDockerClient();
        DockerClientService dockerClientService = new DockerClientService(dockerClient);

        DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName(DOCKER_MCP_SERVER)
                .serverVersion(DOCKER_MCP_SERVER_VER)
                .build());
        mediator.registerHandler(McpServiceFactory.create(dockerClientService)
                .createForNonAnnotatedMethods(false)

                .build());
        mediator.initialize();
    }

    private static DockerClient intitializeDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();

        DockerHttpClient client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();

        return DockerClientImpl.getInstance(config, client);
    }
}
