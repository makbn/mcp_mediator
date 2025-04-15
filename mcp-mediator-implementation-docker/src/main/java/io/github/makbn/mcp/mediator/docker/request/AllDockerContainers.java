package io.github.makbn.mcp.mediator.docker.request;

import io.github.makbn.mcp.mediator.api.McpTool;
import lombok.Getter;


@Getter
@McpTool(name = "get_all_containers",
        description = "get all docker containers. It can include the stopped one by providing load all containers as true",
        schema = AllDockerContainers.class,
        annotations = {
        @McpTool.McpAnnotation(
                title = "calling this tool without without load all container only returns currently running docker" +
                        "containers while passing true will load all the created, paused, and other containers",
                // getting the list of containers can't change anything, right?
                readOnlyHint = true,
                // repeated calls have not additional effects
                idempotentHint = true)
        })
public class AllDockerContainers extends AbstractDockerRequest{
    boolean loadAllContainers;
}
