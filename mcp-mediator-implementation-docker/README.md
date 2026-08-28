# MCP Mediator Docker Implementation

This module provides Docker service integration for the MCP Mediator. It allows you to expose Docker container and image management operations as MCP tools to LLMs.

## Exposed Tools
- `list_containers`: List all Docker containers.
- `list_images`: List all Docker images.
- `start_container`: Start a Docker container by ID.
- `stop_container`: Stop a Docker container by ID.
- `remove_container`: Remove a Docker container by ID.

## Usage
Simply initialize the `DockerMcpService` with your configured `DockerClient` and register it using the `McpServiceFactory`.
