package io.github.makbn.mcp.mediator.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import io.github.makbn.mcp.mediator.api.McpService;
import io.github.makbn.mcp.mediator.api.McpTool;

import java.util.List;

@McpService(name = "docker_mcp_server")
public class DockerMcpService {

    private final DockerClient dockerClient;

    public DockerMcpService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @McpTool(name = "list_containers", description = "List all Docker containers")
    public List<Container> listContainers(boolean showAll) {
        return dockerClient.listContainersCmd().withShowAll(showAll).exec();
    }

    @McpTool(name = "list_images", description = "List all Docker images")
    public List<Image> listImages(boolean showAll) {
        return dockerClient.listImagesCmd().withShowAll(showAll).exec();
    }

    @McpTool(name = "start_container", description = "Start a Docker container by ID")
    public String startContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
        return "Started container: " + containerId;
    }

    @McpTool(name = "stop_container", description = "Stop a Docker container by ID")
    public String stopContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
        return "Stopped container: " + containerId;
    }

    @McpTool(name = "remove_container", description = "Remove a Docker container by ID")
    public String removeContainer(String containerId) {
        dockerClient.removeContainerCmd(containerId).exec();
        return "Removed container: " + containerId;
    }
}
