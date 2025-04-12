package io.github.makbn.mcp.mediator.docker.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpRequest;
import io.github.makbn.mcp.mediator.api.McpRequestHandler;
import io.github.makbn.mcp.mediator.docker.request.DockerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Base Docker MCP request handler that defines tools for Docker operations.
 * Follows the Model Context Protocol specification.
 */
public class DockerMcpRequestHandler implements McpRequestHandler<DockerRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(DockerMcpRequestHandler.class);
    private final DockerClient dockerClient;

    public DockerMcpRequestHandler() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.dockerClient = DockerClientBuilder.getInstance(config).build();
    }

    @Override
    public String getRequestType() {
        return "docker";
    }

    @Override
    public String getName() {
        return "docker";
    }

    @Override
    public boolean canHandle(McpRequest request) {
        return request instanceof DockerRequest;
    }

    @Override
    public Object handle(DockerRequest request) throws McpMediatorException {
        try {
            switch (request.getTool()) {
                // Container operations
                case "create_container":
                    return createContainer(
                        request.getStringParam("image"),
                        request.getStringParam("name"),
                        request.getStringArrayParam("command"),
                        request.getStringArrayParam("env")
                    );
                case "start_container":
                    return startContainer(request.getStringParam("container_id"));
                case "stop_container":
                    return stopContainer(
                        request.getStringParam("container_id"),
                        request.getIntegerParam("timeout", 10)
                    );
                case "restart_container":
                    return restartContainer(
                        request.getStringParam("container_id"),
                        request.getIntegerParam("timeout", 10)
                    );
                case "remove_container":
                    return removeContainer(
                        request.getStringParam("container_id"),
                        request.getBooleanParam("force"),
                        request.getBooleanParam("remove_volumes")
                    );
                case "list_containers":
                    return listContainers(request.getBooleanParam("all"));
                case "inspect_container":
                    return inspectContainer(request.getStringParam("container_id"));
                case "container_logs":
                    return getContainerLogs(
                        request.getStringParam("container_id"),
                        request.getBooleanParam("follow"),
                        request.getBooleanParam("stdout"),
                        request.getBooleanParam("stderr")
                    );

                // Image operations
                case "pull_image":
                    return pullImage(
                        request.getStringParam("image"),
                        request.getStringParam("tag")
                    );
                case "list_images":
                    return listImages(request.getBooleanParam("all"));
                case "remove_image":
                    return removeImage(
                        request.getStringParam("image_id"),
                        request.getBooleanParam("force")
                    );
                case "inspect_image":
                    return inspectImage(request.getStringParam("image_id"));

                // Network operations
                case "create_network":
                    return createNetwork(
                        request.getStringParam("name"),
                        request.getStringParam("driver")
                    );
                case "list_networks":
                    return listNetworks();
                case "remove_network":
                    return removeNetwork(request.getStringParam("network_id"));
                case "inspect_network":
                    return inspectNetwork(request.getStringParam("network_id"));
                case "connect_container_to_network":
                    return connectContainerToNetwork(
                        request.getStringParam("container_id"),
                        request.getStringParam("network_id")
                    );
                case "disconnect_container_from_network":
                    return disconnectContainerFromNetwork(
                        request.getStringParam("container_id"),
                        request.getStringParam("network_id")
                    );

                // Volume operations
                case "create_volume":
                    return createVolume(request.getStringParam("name"));
                case "list_volumes":
                    return listVolumes();
                case "remove_volume":
                    return removeVolume(request.getStringParam("volume_name"));
                case "inspect_volume":
                    return inspectVolume(request.getStringParam("volume_name"));

                default:
                    throw new McpMediatorException(
                        String.format("Unknown Docker tool: %s", request.getTool()));
            }
        } catch (Exception e) {
            throw new McpMediatorException(
                String.format("Failed to execute Docker tool: %s", request.getTool()),
                e);
        }
    }

    // Container operations
    private String createContainer(String image, String name, String[] command, String[] env) {
        LOG.info("Creating Docker container: {}", name);
        CreateContainerResponse container = dockerClient
            .createContainerCmd(image)
            .withName(name)
            .withCmd(command)
            .withEnv(env)
            .exec();
        LOG.info("Created Docker container: {}", container.getId());
        return container.getId();
    }

    private void startContainer(String containerId) {
        LOG.info("Starting container: {}", containerId);
        dockerClient.startContainerCmd(containerId).exec();
    }

    private void stopContainer(String containerId, int timeout) {
        LOG.info("Stopping container: {} with timeout: {}", containerId, timeout);
        dockerClient.stopContainerCmd(containerId).withTimeout(timeout).exec();
    }

    private void restartContainer(String containerId, int timeout) {
        LOG.info("Restarting container: {} with timeout: {}", containerId, timeout);
        dockerClient.restartContainerCmd(containerId).withTimeout(timeout).exec();
    }

    private void removeContainer(String containerId, boolean force, boolean removeVolumes) {
        LOG.info("Removing container: {} (force: {}, remove volumes: {})", 
            containerId, force, removeVolumes);
        dockerClient.removeContainerCmd(containerId)
            .withForce(force)
            .withRemoveVolumes(removeVolumes)
            .exec();
    }

    private List<Container> listContainers(boolean all) {
        LOG.info("Listing Docker containers (all: {})", all);
        return dockerClient.listContainersCmd()
            .withShowAll(all)
            .exec();
    }

    private InspectContainerResponse inspectContainer(String containerId) {
        LOG.info("Inspecting container: {}", containerId);
        return dockerClient.inspectContainerCmd(containerId).exec();
    }

    private String getContainerLogs(String containerId, boolean follow, boolean stdout, boolean stderr) {
        LOG.info("Getting logs for container: {}", containerId);
        return dockerClient.logContainerCmd(containerId)
            .withFollowStream(follow)
            .withStdOut(stdout)
            .withStdErr(stderr)
            .exec()
            .toString();
    }

    // Image operations
    private String pullImage(String image, String tag) {
        LOG.info("Pulling image: {}:{}", image, tag);
        return dockerClient.pullImageCmd(image)
            .withTag(tag)
            .exec(new PullImageResultCallback())
            .awaitCompletion()
            .toString();
    }

    private List<Image> listImages(boolean all) {
        LOG.info("Listing Docker images (all: {})", all);
        return dockerClient.listImagesCmd()
            .withShowAll(all)
            .exec();
    }

    private void removeImage(String imageId, boolean force) {
        LOG.info("Removing image: {} (force: {})", imageId, force);
        dockerClient.removeImageCmd(imageId)
            .withForce(force)
            .exec();
    }

    private InspectImageResponse inspectImage(String imageId) {
        LOG.info("Inspecting image: {}", imageId);
        return dockerClient.inspectImageCmd(imageId).exec();
    }

    // Network operations
    private String createNetwork(String name, String driver) {
        LOG.info("Creating Docker network: {}", name);
        return dockerClient.createNetworkCmd()
            .withName(name)
            .withDriver(driver)
            .exec()
            .getId();
    }

    private List<Network> listNetworks() {
        LOG.info("Listing Docker networks");
        return dockerClient.listNetworksCmd().exec();
    }

    private void removeNetwork(String networkId) {
        LOG.info("Removing network: {}", networkId);
        dockerClient.removeNetworkCmd(networkId).exec();
    }

    private Network inspectNetwork(String networkId) {
        LOG.info("Inspecting network: {}", networkId);
        return dockerClient.inspectNetworkCmd()
            .withNetworkId(networkId)
            .exec();
    }

    private void connectContainerToNetwork(String containerId, String networkId) {
        LOG.info("Connecting container {} to network {}", containerId, networkId);
        dockerClient.connectToNetworkCmd()
            .withContainerId(containerId)
            .withNetworkId(networkId)
            .exec();
    }

    private void disconnectContainerFromNetwork(String containerId, String networkId) {
        LOG.info("Disconnecting container {} from network {}", containerId, networkId);
        dockerClient.disconnectFromNetworkCmd()
            .withContainerId(containerId)
            .withNetworkId(networkId)
            .exec();
    }

    // Volume operations
    private String createVolume(String name) {
        LOG.info("Creating volume: {}", name);
        return dockerClient.createVolumeCmd()
            .withName(name)
            .exec()
            .getName();
    }

    private List<Volume> listVolumes() {
        LOG.info("Listing Docker volumes");
        return dockerClient.listVolumesCmd().exec().getVolumes();
    }

    private void removeVolume(String volumeName) {
        LOG.info("Removing volume: {}", volumeName);
        dockerClient.removeVolumeCmd(volumeName).exec();
    }

    private Volume inspectVolume(String volumeName) {
        LOG.info("Inspecting volume: {}", volumeName);
        return dockerClient.inspectVolumeCmd(volumeName).exec();
    }
} 