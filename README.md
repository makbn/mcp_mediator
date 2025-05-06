# MCP Mediator

A Java-based implementation of the Model Context Protocol (MCP) mediator, providing seamless integration between MCP clients and servers. 
This project enables efficient communication and tool execution in the MCP ecosystem.

![MCP Mediator High Level Diagram](https://github.com/makbn/mcp_mediator/blob/master/.github/static/mcp_mediator_high_level_dark.drawio.png?raw=true#gh-dark-mode-only)
![MCP Mediator High Level Diagram](https://github.com/makbn/mcp_mediator/blob/master/.github/static/mcp_mediator_high_level_light.drawio.png?raw=true#gh-light-mode-only)

## Overview

The MCP Mediator implements the Model Context Protocol specification, providing a robust framework for:
- Handling MCP requests and responses
- Managing tool execution
- Supporting various transport mechanisms
- Integrating with Spring Framework and Spring Boot

For more information, visit the project's wiki: [MCP Mediator Wiki](https://github.com/makbn/mcp_mediator/wiki).
Wiki page is organized as follows:
- [Getting Started](https://github.com/makbn/mcp_mediator/wiki/1‐Getting-Started)  
- [Basic Usage](https://github.com/makbn/mcp_mediator/wiki/2‐Basic-Usage)  
- [Configuration](https://github.com/makbn/mcp_mediator/wiki/3‐Configuration)  
- [Components](https://github.com/makbn/mcp_mediator/wiki/4‐Components)  



## Features

:white_check_mark: Ready:
- Support for stdio/sse transport
- Extensible request handling system
- Configurable server capabilities
- Support for multiple tool implementations
- Support for proxying multiple MCP servers
- Automatically generating MCP Server Tools for the existing methods and services

:hourglass_flowing_sand: Work In Progress:
-  Spring Framework and Spring Boot integration
-  Comprehensive error handling
-  Docker Implementation
-  Dropbox Implementation

:baby_bottle: Planned:
- Generate MCP Server for existing Spring `Controllers` and mediate request between MCP client and controllers
- Generate MCP Server for existing OpenAPI specification and generate MCP `Tool` for the APIs and delegate the requests  

## Modules

- `mcp-mediator-api`: Core API interfaces and contracts
- `mcp-mediator-core`: Core implementation and common functionality
- `mcp-mediator-commons`: Reusable components to make implementation easier
- `mcp-mediator-example`: Example shows how to use and extend the mediator framework
- `mcp-mediator-spring`: Spring Framework and Spring AI integration
- `mcp-mediator-spring-boot-starter`: Spring Boot `autoconfiguration` to generate MCP server automatically for the available endpoints
- Implementation modules for various services:
  - `mcp-mediator-implementation-docker`: Docker service integration [Read More](mcp-mediator-implementation-docker/README.md)
  - `mcp-mediator-implementation-dropbox`: Dropbox service integration (WIP)
  - `mcp-mediator-implementation-query`: Query services with public APIs: [List of all Query MCP Servers](#implemented-query-mcp-servers)

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Spring Boot 3.2.2 or later (for Spring Boot integration)

All the examples are available under `mcp-mediator-exmple` module. It's still a work in progress and the examples will be added.

## Default MCP Mediator 
To create a MCP Mediator with STDIO transport:

```java
DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
          .createDefault()
          .serverName(MY_EXAMPLE_MCP_SERVER_STDIO)
          .build());
mediator.registerHandler(new WikipediaQueryRequestHandler());
mediator.initialize();
```

To run the examples as Claude Desktop MCP Server:
```yaml
{
  "mcpServers": {
    "my_java_mcp_server": {
      "command": "[path to]/run.sh",
      "args": [
        "DefaultMcpMediatorStdioExample"
      ]
    }
  }
}
```
This config runs `DefaultMcpMediatorStdioExample` sa STDIO MCP server. 
Make sure to make `run.sh` executable and add `mvn` command to your path.
```bash
 ./run.sh ClassName [arg1 arg2 ...]
```

This mediator runs a STDIO MCP server with handlers and delegates requests from MCP client  (e.g. Claude Desktop) to
the registered handlers. 

## Convert Existing Code to MCP Server
Convert the existing code, service, helper class, or method automatically to an MCP server using `@McpService`: 
```java
DefaultMcpMediator mediator = new DefaultMcpMediator(McpMediatorConfigurationBuilder.builder()
                .createDefault()
                .serverName(MY_EXAMPLE_MCP_SERVER_STDIO)
                .serverVersion("1.0.0.0")
                .build());
// DockerClientService is an existing service class to interact with the installed Docker Client
        mediator.registerHandler(McpServiceFactory.create(new DockerClientService(dockerClient))
                .createForNonAnnotatedMethods(false)
                .build());
        mediator.initialize();
```

DockerClientService:

```java
@McpService(name = "docker_mcp_server",  description = "provides common docker command as mcp tools")
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
    // ...
}
```

Checkout `mcp-mediator-implementation-docker` for more details.

##  Proxy MCP Mediator
To create a proxy server:
```java
 // as an example ~/sdk/jdk/jdk-17.0.14+7/Contents/Home/bin/java
String command = args[0];
// e.g., -jar ~/mcp-mediator-example.jar
List<String> remoteServerArgs = List.of(Arrays.copyOfRange(args, 1, args.length));

ProxyMcpMediator mediator = new ProxyMcpMediator(McpMediatorConfigurationBuilder.builder()
       .creatProxy()
       .serializer(new ObjectMapper())
       .tools(true)
       .addRemoteServer(McpMediatorProxyConfiguration.McpMediatorRemoteMcpServerConfiguration.builder()
               .remoteTransportType(McpTransportType.STDIO)
               .remoteServerAddress(command)
               .remoteServerArgs(remoteServerArgs)
               .build())
       .serverName(MY_EXAMPLE_MCP_SERVER_STDIO)
       .serverVersion("1.0.0.0")
       .build());

mediator.initialize();
```
This mediator creates a Proxy MCP server and connects to all the given remote servers and works as a proxy between the 
client and the remote servers. The mediator will advertise all the available `Tools` to the clients during the 
initialization process. It is also expandable by registering request handlers using `ProxyMcpMediator#registerHandler()`
same as `DefaultMcpMediator`.


### Implemented Query MCP Servers

Query MCP servers are MCP servers that receive a query from the user and search a source for the content related to the input 
query. All the queries are part of `mcp-mediator-implementation-query` module and are defined using MCP Mediator Request Handlers.
So far, these queries are implemented:
- Wikipedia MCP Server: Search Wikipedia with an input query
- Stackoverflow MCP Server: Search Stackoverflow website
- GitHub MCP Server: Search for GitHub projects


## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. [Read this first](CONTRIBUTING.md)!

## License

This project is licensed under the GPL3 License - see the [LICENSE](https://choosealicense.com/licenses/gpl-3.0/) file for details.
