package io.github.makbn.mcp.mediator.docker.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpRequest;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.docker.request.DockerContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for Docker container requests.
 */
public class DockerContainerHandler implements McpRequestHandler<DockerContainerRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerHandler.class);
    private final DockerClient dockerClient;

    public DockerContainerHandler() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.dockerClient = DockerClientBuilder.getInstance(config).build();
    }

    @Override
    public String getRequestType() {
        return "docker.container.create";
    }

    @Override
    public String getImplementationName() {
        return "docker";
    }

    @Override
    public boolean canHandle(McpRequest request) {
        return request instanceof DockerContainerRequest;
    }

    @Override
    public CreateContainerResponse handle(DockerContainerRequest request) throws McpMediatorException {
        LOG.info("Creating Docker container: {}", request.getName());

        try {
            // Create container
            CreateContainerResponse container = dockerClient
                .createContainerCmd(request.getImage())
                .withName(request.getName())
                .withCmd(request.getCommand())
                .withEnv(request.getEnv())
                .withExposedPorts(ExposedPort.tcp(80))
                .exec();

            LOG.info("Created Docker container: {}", container.getId());
            return container;
        } catch (Exception e) {
            throw new McpMediatorException(
                String.format("Failed to create Docker container: %s", request.getName()),
                e);
        }
    }
} 