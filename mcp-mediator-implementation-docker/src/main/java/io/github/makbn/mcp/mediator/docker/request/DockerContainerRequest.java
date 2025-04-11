package io.github.makbn.mcp.mediator.docker.request;

import io.github.makbn.mcp.mediator.api.McpRequest;

/**
 * Request to create a Docker container.
 */
public class DockerContainerRequest implements McpRequest {
    private final String image;
    private final String name;
    private final String[] command;
    private final String[] env;

    public DockerContainerRequest(String image, String name, String[] command, String[] env) {
        this.image = image;
        this.name = name;
        this.command = command;
        this.env = env;
    }

    @Override
    public String getType() {
        return "docker.container.create";
    }

    @Override
    public String getImplementationName() {
        return "docker";
    }

    public String getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String[] getCommand() {
        return command;
    }

    public String[] getEnv() {
        return env;
    }
} 