package io.github.makbn.mcp.mediator.docker.request;

import lombok.Getter;
import io.github.makbn.mcp.mediator.api.McpTool;

/**
 * MCP Request to perform an action (start, stop, restart, remove, etc.) on a Docker container.
 *
 * @author Matt Akbarian
 */
@Getter
@McpTool(
        name = "change_docker_container_status",
        description = "Perform actions like start, stop, restart, or remove on a Docker container.",
        schema = ChangeDockerContainerStatusMcpRequest.class,
        annotations = {
                @McpTool.McpAnnotation(
                        title = "Allows changing the state of a Docker container, such as starting, stopping, " +
                                "restarting, or removing it. Start command can't be forced. also remove command " +
                                "does not support timeout",
                        destructiveHint = true
                )
        }
)
public class ChangeDockerContainerStatusMcpRequest extends AbstractDockerMcpRequest {

    /**
     * The container ID or name to operate on. Required.
     */
    String containerIdOrName;

    /**
     * The action to perform: start, stop, restart, remove. Required.
     */
    Action action;

    /**
     * Force flag (optional). Used for stop/remove actions to force the operation.
     */
    boolean force;

    /**
     * Timeout in seconds (optional). Used for stop operations to wait before force killing.
     */
    Integer timeout;

    /**
     * Enumeration of valid actions that can be performed on a Docker container.
     */
    public enum Action {
        START,
        STOP,
        RESTART,
        REMOVE
    }
}
