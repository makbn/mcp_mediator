# MCP Mediator

A Java-based implementation of the Model Context Protocol (MCP) mediator, providing seamless integration between MCP clients and servers. 
This project enables efficient communication and tool execution in the MCP ecosystem.

## Overview

The MCP Mediator implements the Model Context Protocol specification, providing a robust framework for:
- Handling MCP requests and responses
- Managing tool execution
- Supporting various transport mechanisms
- Integrating with Spring Framework and Spring Boot

## Features

- Support for stdio transport
- Extensible request handling system
- Spring Framework and Spring Boot integration (WIP)
- Comprehensive error handling (WIP)
- Configurable server capabilities
- Support for multiple tool implementations

## Modules

- `mcp-mediator-api`: Core API interfaces and contracts
- `mcp-mediator-core`: Core implementation and common functionality
- `mcp-mediator-spring`: Spring Framework and Spring AI integration
- `mcp-mediator-spring-boot-starter`: Spring Boot auto-configuration to generate MCP server automatically for the available endpoints
- Implementation modules for various services:
  - `mcp-mediator-implementation-docker`: Docker service integration (WIP)
  - `mcp-mediator-implementation-dropbox`: Dropbox service integration (WIP)

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Spring Boot 3.2.2 or later (for Spring Boot integration)

All the examples are available under `mcp-mediator-exmple` module. It's still a work in progress and the examples will be added.
### Default MCP Mediator 
To create a MCP Mediator with STDIO transport:
```java
      DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName(MY_EXAMPLE_MCP_SERVER_STDIO)
                .build());
        mediator.registerHandler(new DockerMcpRequestHandler());
        mediator.initialize();

```
This mediator runs a STDIO MCP server that handlers delegates requests from MCP client  (e.g. Claude Desktop app) to
the `DockerMcpRequestHandler`. A simple example query :

```text
show all my docker containers
```
and MCP Client (Claude Desktop) interaction with the Mediator:
<details>
 <summary>I'll help you see all your Docker containers. Let me retrieve that information for you. </summary>

```
$ View result from get_all_containers
```

```json
{
   `loadAllContainers`: true
}
{
  "containers": [
      {
      "id": "61d89a44e4b5",
      "name": "nginx-proxy",
      "status": "running"
      },
      {
      "id": "2c5bfa3b0d93",
      "name": "gcr.io/k8s-minikube/kicbase:v0.0.46",
      "status": "running"
      },
       // ...
  ]
}
```
Here are all your Docker containers, including both running and non-running containers:

<table>
  <thead>
      <tr>
          <th>CONTAINER ID</th>
          <th>NAME</th>
          <th>STATUS</th>
      </tr>
  </thead>
  <tbody>
      <tr>
          <td>61d89a44e4b5</td>
          <td>nginx-proxy</td>
          <td>running</td>
      </tr>
      <tr>
          <td>2c5bfa3b0d93</td>
          <td>gcr.io/k8s-minikube/kicbase:v0.0.46</td>
          <td>running</td>
      </tr>
      <tr>
          <td>...</td>
          <td>...</td>
          <td>...</td>
      </tr>
  </tbody>
</table>
You have 10 containers total: 6 running, 2 exited, and 1 paused.
</details>


## Architecture

The MCP Mediator follows the Model Context Protocol architecture:

1. **Protocol Layer**
   - Handles message framing
   - Manages request/response patterns
   - Implements JSON-RPC 2.0

2. **Transport Layer**
   - Supports stdio transport
   - Handles message serialization/deserialization
   - Manages connection lifecycle

3. **Tool Layer**
   - Implements tool execution
   - Handles request routing
   - Manages tool capabilities

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. [Read this first](CONTRIBUTING.md)!

## License

This project is licensed under the GPL3 License - see the [LICENSE](https://choosealicense.com/licenses/gpl-3.0/) file for details.
