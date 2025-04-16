package io.github.makbn.mcp.mediator.docker.request;

import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.docker.request.result.DockerMcpResult;

/**
 * Docker request that follows the Model Context Protocol specification.
 * Represents a request to execute a Docker tool with its parameters.
 */
public abstract class AbstractDockerMcpRequest implements McpMediatorRequest<DockerMcpResult> {

}