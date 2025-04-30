# MCP Mediator Docker Implementation

This module provides a Docker-based implementation of an MCP (Message Communication Protocol) server using the [`mcp_mediator`](https://github.com/makbn/mcp_mediator) core framework.

It acts as a pluggable server that leverages Docker's API to handle MCP messages using mediator-driven handler registration and execution.

### Usage Examples

```shell
java -jar docker-mcp-server.jar \
  --docker-host=tcp://localhost:2376 \
  --tls-verify \
  --cert-path=/etc/docker/certs \
  --server-name=my-server \
  --server-version=1.0.0 \
  --max-connections=150 \
  --docker-config=/custom/docker/config
```


To run Docker MCP Server with Claude Desktop:
```yaml
{
  "mcpServers": {
    "my_java_mcp_server": {
      "command": "java",
      "args": [
        "-jar",
        "--docker-host=tcp://localhost:2376",
        "--tls-verify", # not required
        "--cert-path=/etc/docker/certs", # required only if --tls-verify is available
        "--server-name=my-docker-mcp-server",
        "--server-version=1.0.0",
        "--max-connections=150",
        "--docker-config=/custom/docker/config"
      ]
    }
  }
}
```


### Supported CLI Options

| Option             | Description                                           | Default                       |
|--------------------|-------------------------------------------------------|-------------------------------|
| `--docker-host`    | Docker daemon host URI                                | `unix:///var/run/docker.sock` |
| `--tls-verify`     | Enable TLS verification (used with `--cert-path`)     | `false`                        |
| `--cert-path`      | Path to Docker TLS client certificates (required if TLS is enabled) | _none_              |
| `--docker-config`  | Custom Docker config directory                         | `~/.docker`                    |
| `--server-name`    | Server name for the MCP server                         | `docker_mcp_server`            |
| `--server-version` | Server version label                                   | `1.0.0.0`                      |
| `--max-connections`| Maximum number of connections to Docker daemon        | `100`                          |
| `--log-level`      | Logging level (`TRACE`, `DEBUG`, `INFO`, etc.)        | `DEBUG`                        |
| `--log-file`       | Path to log output file                                | `logs/example.log`             |
| `--help`           | Show usage and available options                       | _n/a_                          |


### Supported CLI Tools
| MCP Tool Name                                | Description                                                                                             |
|---------------------------------------------|---------------------------------------------------------------------------------------------------------|
| docker_start_container                       | Start an existing Docker container using its containerId.                                               |
| docker_stop_container                        | Stop an existing Docker container using its containerId.                                                |
| docker_leave_swarm                           | Remove a node from a Docker Swarm.                                                                      |
| docker_container_diff                        | List changes made to a container’s filesystem since creation.                                           |
| docker_build_image_file                      | Build a Docker image from a Dockerfile or directory.                                                    |
| docker_inspect_volume                        | Retrieve detailed information about a Docker volume by name.                                            |
| docker_remove_service                        | Remove a Docker service by its serviceId.                                                               |
| docker_list_containers                       | List Docker containers (optionally filtered and showing all or just running ones).                      |
| docker_inspect_swarm                         | Inspect the current Docker Swarm details.                                                               |
| docker_push_image                            | Push an image to a registry (authentication optional).                                                  |
| docker_copy_archive_to_container             | Copy a tar archive into a running container.                                                            |
| docker_stats_container                       | Retrieve real-time or snapshot statistics of a container.                                               |
| docker_disconnect_container_from_network     | Disconnect a container from a Docker network.                                                           |
| docker_remove_container                      | Remove a container, optionally forcibly if it's running.                                                |

### DockerClientService Function Coverage
<details>
<summary>List of all supported functionality and the current state of implementation</summary>
| Function Name                                | MCP Tool Name                            | Status        |
|---------------------------------------------|------------------------------------------|---------------|
| startContainerCmd                            | docker_start_container                   | ✅ Implemented |
| stopContainerCmd                             | docker_stop_container                    | ✅ Implemented |
| leaveSwarmCmd                                | docker_leave_swarm                       | ✅ Implemented |
| attachContainerCmd                           | N/A                                      | ❌ Not Yet     |
| containerDiffCmd                             | docker_container_diff                    | ✅ Implemented |
| buildImageCmd                                | docker_build_image_file                  | ✅ Implemented |
| inspectVolumeCmd                             | docker_inspect_volume                    | ✅ Implemented |
| removeServiceCmd                             | docker_remove_service                    | ✅ Implemented |
| listContainersCmd                            | docker_list_containers                   | ✅ Implemented |
| inspectSwarmCmd                              | docker_inspect_swarm                     | ✅ Implemented |
| pushImageCmd                                 | docker_push_image                        | ✅ Implemented |
| copyArchiveToContainerCmd                    | docker_copy_archive_to_container         | ✅ Implemented |
| statsCmd                                     | docker_stats_container                   | ✅ Implemented |
| disconnectFromNetworkCmd                     | docker_disconnect_container_from_network | ✅ Implemented |
| removeContainerCmd                           | docker_remove_container                  | ✅ Implemented |
| inspectServiceCmd                            | N/A                                      | ❌ Not Yet     |
| removeSecretCmd                              | N/A                                      | ❌ Not Yet     |
| pullImageCmd                                 | N/A                                      | ❌ Not Yet     |
| inspectContainerCmd                          | N/A                                      | ❌ Not Yet     |
| unpauseContainerCmd                          | N/A                                      | ❌ Not Yet     |
| listImagesCmd                                | N/A                                      | ❌ Not Yet     |
| listServicesCmd                              | N/A                                      | ❌ Not Yet     |
| resizeExecCmd                                | N/A                                      | ❌ Not Yet     |
| listSecretsCmd                               | N/A                                      | ❌ Not Yet     |
| removeImageCmd                               | N/A                                      | ❌ Not Yet     |
| createNetworkCmd                             | N/A                                      | ❌ Not Yet     |
| tagImageCmd                                  | N/A                                      | ❌ Not Yet     |
| authCmd                                      | N/A                                      | ❌ Not Yet     |
| execCreateCmd                                | N/A                                      | ❌ Not Yet     |
| removeSwarmNodeCmd                           | N/A                                      | ❌ Not Yet     |
| inspectConfigCmd                             | N/A                                      | ❌ Not Yet     |
| searchImagesCmd                              | N/A                                      | ❌ Not Yet     |
| listNetworksCmd                              | N/A                                      | ❌ Not Yet     |
| pingCmd                                      | N/A                                      | ❌ Not Yet     |
| logTaskCmd                                   | N/A                                      | ❌ Not Yet     |
| loadImageAsyncCmd                            | N/A                                      | ❌ Not Yet     |
| updateSwarmCmd                               | N/A                                      | ❌ Not Yet     |
| removeVolumeCmd                              | N/A                                      | ❌ Not Yet     |
| close                                        | N/A                                      | ❌ Not Yet     |
| createContainerCmd                           | N/A                                      | ❌ Not Yet     |
| buildImageCmd(InputStream)                   | N/A                                      | ❌ Not Yet     |
| loadImageCmd                                 | N/A                                      | ❌ Not Yet     |
| listTasksCmd                                 | N/A                                      | ❌ Not Yet     |
| saveImagesCmd                                | N/A                                      | ❌ Not Yet     |
| joinSwarmCmd                                 | N/A                                      | ❌ Not Yet     |
| createVolumeCmd                              | N/A                                      | ❌ Not Yet     |
| buildImageCmd()                              | N/A                                      | ❌ Not Yet     |
| saveImageCmd                                 | N/A                                      | ❌ Not Yet     |

</details>


### 🧩 Repository Structure and Git Subtree Setup

This project is a **Git subtree module** of the parent repository [`makbn/mcp_mediator`](https://github.com/makbn/mcp_mediator). It is kept in its own repository to support independent versioning, CI, and release processes, while remaining integrated into the main `mcp_mediator` mono-repo.

### 🔀 Cloning Structure

If you're working in the context of the full `mcp_mediator` system:

```bash
git clone https://github.com/makbn/mcp_mediator.git
cd mcp_mediator


