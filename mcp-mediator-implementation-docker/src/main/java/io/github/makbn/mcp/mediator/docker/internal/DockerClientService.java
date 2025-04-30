package io.github.makbn.mcp.mediator.docker.internal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpService;
import io.github.makbn.mcp.mediator.api.McpTool;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@McpService(
        name = "docker_mcp_server",
        description = "provides common docker command as mcp tools"
)
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DockerClientService {

    DockerClient internalClient;

    @McpTool(name = "docker_start_container",
            description = "start an existing docker container using its containerId! containerId is required and " +
                    "can't be null or empty! if successful, returns true.")
    public boolean startContainerCmd(@NonNull String containerId) {
        internalClient.startContainerCmd(containerId)
                .exec();
        return true;
    }

    @McpTool(name = "docker_stop_container",
            description = "stop an existing docker container using its containerId! containerId is required and " +
                    "can't be null or empty! if successful, returns true.")
    public boolean stopContainerCmd(String containerId) {
        internalClient.stopContainerCmd(containerId)
                .exec();
        return true;
    }

    @McpTool(name = "docker_leave_swarm",
            description = "The docker swarm leave command is used to remove a node from a Docker Swarm. " +
                    "When executed on a node, it disconnects the node from the swarm, and it will no longer receive " +
                    "tasks from the swarm manager. no args is needed for this command")
    public boolean leaveSwarmCmd(boolean force) {
        LeaveSwarmCmd cmd = internalClient.leaveSwarmCmd();
        if (force) {
            cmd.withForceEnabled(true);
        }
        cmd.exec();

        return true;
    }

    // not supported
    public AttachContainerCmd attachContainerCmd(@NonNull String containerId) {
        return internalClient.attachContainerCmd(containerId);
    }

    @McpTool(name = "docker_container_diff",
            description = "List changes made to a container's filesystem since creation. " +
                    "Shows added (A), deleted (D), and changed (C) files. containerId is required and can't be null " +
                    "or empty",
            schema = ChangeLog[].class)
    public List<ChangeLog> containerDiffCmd(String containerId) {
        return internalClient.containerDiffCmd(containerId)
                .exec();
    }

    @McpTool(name = "docker_build_image_file",
            description = "Build a Docker image from a Dockerfile or directory and returns the created imageId or" +
                    "error message in case of any exception. dockerFileOrFolderPath is the absolute path to the" +
                    "Dockerfile or the base directory that contains Dockerfile. baseDirectoryPath is the path to" +
                    "execute build and usually is the same as dockerFileOrFolderPath if not explicitly specified." +
                    "tags is a set of unique tags for the image name and tag. at least one tag should be in the tags." +
                    "build args are a list of key value pairs to be passed to the build. platform sets the target" +
                    "platform for the build.")
    public String buildImageCmd(String dockerFileOrFolderPath, String baseDirectoryPath, Set<String> tags, Map<String, String> buildArgs, String platform) throws ExecutionException, InterruptedException, TimeoutException {
        BuildImageCmd cmd = internalClient.buildImageCmd(new File(dockerFileOrFolderPath))
                .withBaseDirectory(new File(baseDirectoryPath));
        cmd.withTags(tags);

        if (buildArgs != null) {
            buildArgs.forEach(cmd::withBuildArg);
        }

        if (platform != null) {
            cmd.withPlatform(platform);
        }

        CompletableFuture<BuildResponseItem> future = new CompletableFuture<>();
        cmd.exec(new ResultCallback<BuildResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {
                // NO-OP
            }

            @Override
            public void onNext(BuildResponseItem item) {
                if (item.isBuildSuccessIndicated()) {
                    future.complete(item);
                } else if (item.isErrorIndicated()) {
                    future.completeExceptionally(new RuntimeException("Error during build: " + item.getErrorDetail()));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                // NO-OP
            }

            @Override
            public void close() {
                // NO-OP
            }
        });

        BuildResponseItem responseItem = future.get(10, TimeUnit.MINUTES);
        return responseItem.getImageId();
    }

    @McpTool(name = "docker_inspect_volume",
            description = "Retrieve detailed information about a Docker volume by name.")
    public InspectVolumeResponse inspectVolumeCmd(String name) {
        return internalClient.inspectVolumeCmd(name)
                .exec();
    }

    @McpTool(name = "docker_remove_service",
            description = "Remove a Docker service by its serviceId. returns true if successful.")
    public boolean removeServiceCmd(String serviceId) {
        internalClient.removeServiceCmd(serviceId)
                .exec();
        return true;
    }

    @McpTool(name = "docker_list_containers",
            description = "List Docker containers. if showAll param is true, shows all the contains, if false " +
                    "shows just running). filter can be passed as key value pairs to filter the output and it can " +
                    "be more than one pair in a request but usually it is not required by default.")
    public List<Container> listContainersCmd(boolean showAll, Map<String, String> filter) {
        ListContainersCmd cmd = internalClient.listContainersCmd();
        cmd.withShowAll(showAll);
        if (filter != null) {
            filter.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                    .forEach(entry ->
                            cmd.withFilter(entry.getKey(), Collections.singletonList(entry.getValue())));

        }

        return cmd.exec();
    }

    @McpTool(
            name = "docker_inspect_swarm",
            description = "Inspect the current Docker Swarm details")
    public Swarm inspectSwarmCmd() {
        return internalClient.inspectSwarmCmd().exec();
    }

    @McpTool(
            name = "docker_push_image",
            description = "Push an image to a registry. registryAddress is the url of registry " +
                    "e.g.: registry.hub.docker.com! pushedImageTag is the name an tag of the image to push!" +
                    "if and only authentication is required, username and password is the credentials to authenticate" +
                    "to the registry, otherwise can be null.",
            schema = PushImageCmd.class
    )
    public PushResponseItem pushImageCmd(@NonNull String registryAddress, @NonNull String pushedImageTag,
                                         @Nullable String username, @Nullable String password)
            throws ExecutionException, InterruptedException, TimeoutException {
        Repository repository = new Repository(registryAddress);
        Identifier identifier = new Identifier(repository, pushedImageTag);

        PushImageCmd cmd = internalClient.pushImageCmd(identifier);
        if (username != null && password != null) {
            AuthConfig authConfig = new AuthConfig();
            authConfig.withUsername(username);
            authConfig.withPassword(password);
            cmd.withAuthConfig(authConfig);
        }

        CompletableFuture<PushResponseItem> future = new CompletableFuture<>();

        cmd.exec(new ResultCallback<PushResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {
                // NO-OP
            }

            @Override
            public void onNext(PushResponseItem object) {
                if (object.isErrorIndicated()) {
                    future.completeExceptionally(
                            new McpMediatorException(Objects.requireNonNullElse(object.getErrorDetail(),
                                    "Error happened during the push process").toString()));
                } else {
                    future.complete(object);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                // NO - OP
            }

            @Override
            public void close() {
                // NO-OP
            }
        });

        return future.get(10, TimeUnit.MINUTES);
    }


    @McpTool(
            name = "docker_copy_archive_to_container",
            description = """
                      Copies a tar archive from the host filesystem into a running container at the specified\s
                      destination path.
                      This can be used to transfer files or directories into the container.
                      All paths must be absolute, and the archive must be in TAR format.
                      Returns true if successful.
                    
                       Parameters:
                     - containerId: (String, required) The ID or name of the container where the archive will be copied.Must not be null or empty.
                     - archiveFileAbsolutePath: (String, required) Absolute path to the tar archive file on the host system. The file must exist and be in TAR format.
                     - containerDestinationPath: (String, required) Destination absolute path inside the container's filesystem where the archive will be extracted.
                    """)
    public boolean copyArchiveToContainerCmd(@NonNull String containerId, @NonNull String archiveFileAbsolutePath,
                                             @NonNull String containerDestinationPath) throws FileNotFoundException {
        CopyArchiveToContainerCmd cmd = internalClient.copyArchiveToContainerCmd(containerId);
        FileInputStream inputStream = new FileInputStream(new File(archiveFileAbsolutePath));
        cmd.withTarInputStream(inputStream);
        cmd.withRemotePath(containerDestinationPath);
        cmd.exec();
        return true;
    }

    @McpTool(
            name = "docker_stats_container",
            description = """
                    Retrieves container statistics, including CPU, memory, network, and disk usage. 
                    This method streams real-time statistics for a container and optionally allows for a one-time snapshot (via the noStream parameter).
                    
                    Parameters:
                    - containerId: (String, required) The ID or name of the container for which the statistics will be fetched. 
                      Must not be null or empty. The container should be running.
                    - noStream: (boolean, required) Whether to disable streaming statistics. 
                      If true, only the current stats will be retrieved without continuous updates. 
                      If false, the stats will be streamed until the connection is closed.
                    """)
    public Statistics statsCmd(@NonNull String containerId, boolean noStream)
            throws ExecutionException, InterruptedException, TimeoutException {
        StatsCmd cmd = internalClient.statsCmd(containerId)
                .withNoStream(noStream);

        CompletableFuture<Statistics> future = new CompletableFuture<>();

        cmd.exec(new ResultCallback<Statistics>() {
            @Override
            public void onStart(Closeable closeable) {
                // NO-OP
            }

            @Override
            public void onNext(Statistics object) {
                future.complete(object);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                // NO-OP
            }

            @Override
            public void close() {
                // NO-OP
            }
        });
        return future.get(10, TimeUnit.MINUTES);

    }

    @McpTool(
            name = "docker_disconnect_container_from_network",
            description = """
                    Disconnects a container from a specified Docker network.
                    If the container is running, it will be immediately disconnected from the network, potentially disrupting any network-dependent functionality.
                    
                    Parameters:
                    - containerId: (String, required) The ID or name of the container to disconnect from the network. 
                      Must not be null or empty. The container must be part of the specified network.
                    - networkId: (String, required) The ID or name of the Docker network to disconnect the container from. 
                      Must not be null or empty.
                    - force: (boolean, required) Whether to force the disconnection of the container from the network even if there are active connections. 
                      If true, the disconnection will proceed regardless of the container's network state.
                    """)
    public boolean disconnectFromNetworkCmd(@NonNull String containerId, @NonNull String networkId, boolean force) {
        internalClient.disconnectFromNetworkCmd()
                .withContainerId(containerId)
                .withNetworkId(networkId)
                .withForce(force)
                .exec();
        return true;
    }


    @McpTool(
            name = "docker_remove_container",
            description = """
                    Removes a container from Docker. If the container is running, you can optionally force it to stop and remove it.
                    This command will remove the container and its associated data, including filesystem changes,\s
                    unless persistent volumes are used.
                    
                    Parameters:
                    - containerId: (String, required) The ID or name of the container to remove. Must not be null or empty.
                      The container must exist in Docker and should either be stopped or forcibly removed if running.
                    - force: (boolean, required) Whether to force the removal of a running container.
                      If true, the container will be stopped and removed, even if it is currently running. If false, the container\s
                      must be stopped manually before removal.
                    """)
    public boolean removeContainerCmd(@NonNull String containerId, boolean force) {
        internalClient.removeContainerCmd(containerId)
                .withForce(force)
                .exec();
        return true;
    }

    public InspectServiceCmd inspectServiceCmd(String serviceId) {
        return internalClient.inspectServiceCmd(serviceId);
    }

    public RemoveSecretCmd removeSecretCmd(String secretId) {
        return internalClient.removeSecretCmd(secretId);
    }

    public PullImageCmd pullImageCmd(String repository) {
        return internalClient.pullImageCmd(repository);
    }

    public InspectContainerCmd inspectContainerCmd(String containerId) {
        return internalClient.inspectContainerCmd(containerId);
    }

    public UnpauseContainerCmd unpauseContainerCmd(String containerId) {
        return internalClient.unpauseContainerCmd(containerId);
    }

    public ListImagesCmd listImagesCmd() {
        return internalClient.listImagesCmd();
    }

    public ListServicesCmd listServicesCmd() {
        return internalClient.listServicesCmd();
    }

    public ResizeExecCmd resizeExecCmd(String execId) {
        return internalClient.resizeExecCmd(execId);
    }

    public ListSecretsCmd listSecretsCmd() {
        return internalClient.listSecretsCmd();
    }

    public RemoveImageCmd removeImageCmd(String imageId) {
        return internalClient.removeImageCmd(imageId);
    }

    public CreateNetworkCmd createNetworkCmd() {
        return internalClient.createNetworkCmd();
    }

    public TagImageCmd tagImageCmd(String imageId, String imageNameWithRepository, String tag) {
        return internalClient.tagImageCmd(imageId, imageNameWithRepository, tag);
    }

    public AuthCmd authCmd() {
        return internalClient.authCmd();
    }

    public ExecCreateCmd execCreateCmd(String containerId) {
        return internalClient.execCreateCmd(containerId);
    }

    public RemoveSwarmNodeCmd removeSwarmNodeCmd(String swarmNodeId) {
        return internalClient.removeSwarmNodeCmd(swarmNodeId);
    }

    public InspectConfigCmd inspectConfigCmd(String configId) {
        return internalClient.inspectConfigCmd(configId);
    }

    public SearchImagesCmd searchImagesCmd(String term) {
        return internalClient.searchImagesCmd(term);
    }

    public ListNetworksCmd listNetworksCmd() {
        return internalClient.listNetworksCmd();
    }

    public PingCmd pingCmd() {
        return internalClient.pingCmd();
    }

    public LogSwarmObjectCmd logTaskCmd(String taskId) {
        return internalClient.logTaskCmd(taskId);
    }

    public LoadImageAsyncCmd loadImageAsyncCmd(InputStream imageStream) {
        return internalClient.loadImageAsyncCmd(imageStream);
    }

    public UpdateSwarmCmd updateSwarmCmd(SwarmSpec swarmSpec) {
        return internalClient.updateSwarmCmd(swarmSpec);
    }

    public RemoveVolumeCmd removeVolumeCmd(String name) {
        return internalClient.removeVolumeCmd(name);
    }

    public void close() throws IOException {
        internalClient.close();
    }

    public CreateContainerCmd createContainerCmd(String image) {
        return internalClient.createContainerCmd(image);
    }

    public BuildImageCmd buildImageCmd(InputStream tarInputStream) {
        return internalClient.buildImageCmd(tarInputStream);
    }

    public LoadImageCmd loadImageCmd(InputStream imageStream) {
        return internalClient.loadImageCmd(imageStream);
    }

    public ListTasksCmd listTasksCmd() {
        return internalClient.listTasksCmd();
    }

    public SaveImagesCmd saveImagesCmd() {
        return internalClient.saveImagesCmd();
    }

    public JoinSwarmCmd joinSwarmCmd() {
        return internalClient.joinSwarmCmd();
    }

    public CreateVolumeCmd createVolumeCmd() {
        return internalClient.createVolumeCmd();
    }

    public BuildImageCmd buildImageCmd() {
        return internalClient.buildImageCmd();
    }

    public SaveImageCmd saveImageCmd(String name) {
        return internalClient.saveImageCmd(name);
    }

    public InitializeSwarmCmd initializeSwarmCmd(SwarmSpec swarmSpec) {
        return internalClient.initializeSwarmCmd(swarmSpec);
    }

    public UpdateServiceCmd updateServiceCmd(String serviceId, ServiceSpec serviceSpec) {
        return internalClient.updateServiceCmd(serviceId, serviceSpec);
    }

    public CommitCmd commitCmd(String containerId) {
        return internalClient.commitCmd(containerId);
    }

    public ListConfigsCmd listConfigsCmd() {
        return internalClient.listConfigsCmd();
    }

    public InspectImageCmd inspectImageCmd(String imageId) {
        return internalClient.inspectImageCmd(imageId);
    }

    public EventsCmd eventsCmd() {
        return internalClient.eventsCmd();
    }

    public ConnectToNetworkCmd connectToNetworkCmd() {
        return internalClient.connectToNetworkCmd();
    }

    public ResizeContainerCmd resizeContainerCmd(String containerId) {
        return internalClient.resizeContainerCmd(containerId);
    }

    public RestartContainerCmd restartContainerCmd(String containerId) {
        return internalClient.restartContainerCmd(containerId);
    }

    public CreateServiceCmd createServiceCmd(ServiceSpec serviceSpec) {
        return internalClient.createServiceCmd(serviceSpec);
    }

    public RemoveNetworkCmd removeNetworkCmd(String networkId) {
        return internalClient.removeNetworkCmd(networkId);
    }

    public CreateSecretCmd createSecretCmd(SecretSpec secretSpec) {
        return internalClient.createSecretCmd(secretSpec);
    }

    public CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(String containerId, String resource) {
        return internalClient.copyArchiveFromContainerCmd(containerId, resource);
    }

    public RenameContainerCmd renameContainerCmd(String containerId) {
        return internalClient.renameContainerCmd(containerId);
    }

    public PauseContainerCmd pauseContainerCmd(String containerId) {
        return internalClient.pauseContainerCmd(containerId);
    }

    public VersionCmd versionCmd() {
        return internalClient.versionCmd();
    }

    public ListSwarmNodesCmd listSwarmNodesCmd() {
        return internalClient.listSwarmNodesCmd();
    }

    public LogContainerCmd logContainerCmd(String containerId) {
        return internalClient.logContainerCmd(containerId);
    }

    public UpdateContainerCmd updateContainerCmd(String containerId) {
        return internalClient.updateContainerCmd(containerId);
    }

    public PruneCmd pruneCmd(PruneType pruneType) {
        return internalClient.pruneCmd(pruneType);
    }

    public InspectNetworkCmd inspectNetworkCmd() {
        return internalClient.inspectNetworkCmd();
    }

    public InspectExecCmd inspectExecCmd(String execId) {
        return internalClient.inspectExecCmd(execId);
    }

    public KillContainerCmd killContainerCmd(String containerId) {
        return internalClient.killContainerCmd(containerId);
    }

    public TopContainerCmd topContainerCmd(String containerId) {
        return internalClient.topContainerCmd(containerId);
    }

    public ListVolumesCmd listVolumesCmd() {
        return internalClient.listVolumesCmd();
    }

    public UpdateSwarmNodeCmd updateSwarmNodeCmd() {
        return internalClient.updateSwarmNodeCmd();
    }

    public RemoveConfigCmd removeConfigCmd(String configId) {
        return internalClient.removeConfigCmd(configId);
    }

    public InfoCmd infoCmd() {
        return internalClient.infoCmd();
    }

    public LogSwarmObjectCmd logServiceCmd(String serviceId) {
        return internalClient.logServiceCmd(serviceId);
    }
}
