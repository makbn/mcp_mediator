package io.github.makbn.mcp.mediator.docker.request;

import io.github.makbn.mcp.mediator.api.McpTool;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * MCP Request for creating a Docker container.
 * This request allows configuring container image, name, ports, environment variables, etc.
 *
 * @author Matt Akbarian
 */
@Getter
@McpTool(
        name = "create_docker_container",
        description = "Create a new Docker container from a specified image with optional settings like ports and environment variables.",
        schema = CreateDockerContainerMcpRequest.class,
        annotations = {
                @McpTool.McpAnnotation(
                        title = "Creates a new Docker container. This operation is not read-only and will result in side effects. " +
                                "The container will not be started automatically unless 'startAfterCreate' is true.",
                        openWorldHint = true
                )
        }
)
public class CreateDockerContainerMcpRequest extends AbstractDockerMcpRequest{
    /**
     * The name of the Docker image to use. Required.
     */
    String image;

    /**
     * Optional name for the container.
     */
    String containerName;

    /**
     * If true, the container will be started right after creation.
     */
    boolean startAfterCreate = false;

    /**
     * Environment variables to be passed to the container, e.g., {"ENV_VAR": "value"}.
     */
    Map<String, String> environmentVariables;

    /**
     * Port mappings, e.g., {"8080": "80"} to map host port 8080 to container port 80.
     */
    Map<String, String> portBindings;

    /**
     * Volume mounts, e.g., {"/host/path": "/container/path"}.
     */
    Map<String, String> volumeMounts;

    /**
     * Command override to run in the container, if specified.
     */
    List<String> command;

    /**
     * Entrypoint override.
     */
    List<String> entrypoint;

    /**
     * Labels to assign to the container.
     */
    Map<String, String> labels;

    /**
     * Working directory inside the container.
     */
    String workingDir;

    /**
     * Restart policy (e.g., "no", "always", "on-failure").
     */
    String restartPolicy;

    /**
     * Network mode (e.g., "bridge", "host", "none").
     */
    String networkMode;
}
