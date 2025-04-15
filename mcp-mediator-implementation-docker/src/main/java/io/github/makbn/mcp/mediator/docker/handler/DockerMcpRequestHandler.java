package io.github.makbn.mcp.mediator.docker.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import io.github.makbn.mcp.mediator.docker.request.AbstractDockerRequest;
import io.github.makbn.mcp.mediator.docker.request.AllDockerContainers;
import io.github.makbn.mcp.mediator.docker.request.result.DockerMcpResult;

import java.util.Collection;
import java.util.List;

/**
 * Base Docker MCP request handler that defines tools for Docker operations.
 * Follows the Model Context Protocol specification.
 *
 * @author Matt Akbarian
 */
public class DockerMcpRequestHandler implements McpMediatorRequestHandler<AbstractDockerRequest, DockerMcpResult> {

    private static final String REQUEST_HANDLER_NAME = "docker-mcp-request-handler";
    private final DockerClient dockerClient;

    public DockerMcpRequestHandler() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.dockerClient = DockerClientImpl.getInstance(config);
    }


    @Override
    public String getName() {
        return REQUEST_HANDLER_NAME;
    }

    @Override
    public boolean canHandle(McpMediatorRequest<?> request) {
        return request instanceof AbstractDockerRequest;
    }

    @Override
    public Collection<Class<? extends AbstractDockerRequest>> getAllSupportedRequestClass() {
        return List.of(AllDockerContainers.class);
    }

    @Override
    public DockerMcpResult handle(AbstractDockerRequest request) throws McpMediatorException {
        try {
           if (request instanceof AllDockerContainers allDockerContainers) {
               return DockerMcpResult.create()
                       .addResult("list_of_containers", getAllContainers(allDockerContainers));
           }

           return DockerMcpResult.create()
                   .addResult("error", "Unknown request type: " + request.getClass().getName());
        } catch (Exception e) {
            throw new McpMediatorException(
                String.format("Failed to execute Docker tool: %s", request),
                e);
        }
    }

    private List<Container> getAllContainers(AllDockerContainers request) {
        ListContainersCmd cmd = dockerClient.listContainersCmd();
        cmd.withShowAll(request.isLoadAllContainers());
        return cmd.exec();
    }
} 