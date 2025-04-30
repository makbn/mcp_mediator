package io.github.makbn.mcp.mediator.example;

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

public class ServiceMcpMediatorStdioExample {

    public static final String MY_EXAMPLE_MCP_SERVER_STDIO = "docker_mcp_server_stdio";

    public static void main(String[] args) {

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();

        DockerHttpClient client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, client);

        DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName(MY_EXAMPLE_MCP_SERVER_STDIO)
                .serverVersion("1.0.0.0")
                .build());
        mediator.registerHandler(McpServiceFactory.create(new DockerClientService(dockerClient))
                .createForNonAnnotatedMethods(false)
                .build());
        mediator.initialize();
    }
}
