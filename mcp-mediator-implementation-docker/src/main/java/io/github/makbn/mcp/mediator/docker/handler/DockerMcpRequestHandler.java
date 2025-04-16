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
import io.github.makbn.mcp.mediator.docker.request.AbstractDockerMcpRequest;
import io.github.makbn.mcp.mediator.docker.request.AllDockerContainersMcpRequest;
import io.github.makbn.mcp.mediator.docker.request.result.DockerMcpResult;

import java.util.Collection;
import java.util.List;

/**
 * Base Docker MCP request handler that defines tools for Docker operations.
 * Follows the Model Context Protocol specification.
 *
 * @author Matt Akbarian
 */
public class DockerMcpRequestHandler implements McpMediatorRequestHandler<AbstractDockerMcpRequest, DockerMcpResult> {

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
        return request instanceof AbstractDockerMcpRequest;
    }

    @Override
    public Collection<Class<? extends AbstractDockerMcpRequest>> getAllSupportedRequestClass() {
        return List.of(AllDockerContainersMcpRequest.class);
    }

    @Override
    public DockerMcpResult handle(AbstractDockerMcpRequest request) throws McpMediatorException {
        try {
           if (request instanceof AllDockerContainersMcpRequest allDockerContainers) {
               return DockerMcpResult.create()
                       .addResult("list_of_containers", getAllContainers(allDockerContainers));
           }

           throw new UnsupportedOperationException("Not implemented yet");
        } catch (UnsupportedOperationException propagatedException) {
            throw propagatedException;
        } catch (Exception e) {
            throw new McpMediatorException(
                String.format("Failed to execute Docker tool: %s", request),
                e);
        }
    }

    private List<Container> getAllContainers(AllDockerContainersMcpRequest request) {
        ListContainersCmd cmd = dockerClient.listContainersCmd();
        cmd.withShowAll(request.isLoadAllContainers());
        return cmd.exec();
    }
} 