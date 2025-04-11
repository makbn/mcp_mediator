# MCP Mediator

A modular MCP mediator framework compatible with Spring Framework and Spring Boot. This project provides a flexible and extensible way to integrate with various MCP (Message Control Protocol) servers.

## Features

- Modular architecture with clear separation of concerns
- Spring Framework and Spring Boot integration
- Extensible API for implementing custom mediators
- Support for multiple MCP server implementations
- Comprehensive documentation and examples

## Modules

- `mcp-mediator-api`: Core API interfaces and contracts
- `mcp-mediator-core`: Base implementation and common functionality
- `mcp-mediator-spring`: Spring Framework integration
- `mcp-mediator-spring-boot-starter`: Spring Boot auto-configuration
- Implementation modules for various MCP servers:
  - AWS
  - Azure
  - Google Cloud Platform
  - Kubernetes
  - Docker

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Spring Boot 3.2.2 or later (for Spring Boot integration)

### Installation

Add the following dependency to your Spring Boot project:

```xml
<dependency>
    <groupId>io.github.makbn</groupId>
    <artifactId>mcp-mediator-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Configuration

Add the following properties to your `application.yml` or `application.properties`:

```yaml
mcp:
  mediator:
    implementation-name: aws  # or azure, gcp, kubernetes, docker
    properties:
      # Implementation-specific properties
```

### Usage

```java
@RestController
public class MyController {
    private final McpMediator mediator;

    public MyController(McpMediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/send")
    public void sendMessage(@RequestBody String message) {
        // Use the mediator to send messages
    }
}
```

## Implementation Modules

### AWS Implementation

```xml
<dependency>
    <groupId>io.github.makbn</groupId>
    <artifactId>mcp-mediator-implementation-aws</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Azure Implementation

```xml
<dependency>
    <groupId>io.github.makbn</groupId>
    <artifactId>mcp-mediator-implementation-azure</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Google Cloud Platform Implementation

```xml
<dependency>
    <groupId>io.github.makbn</groupId>
    <artifactId>mcp-mediator-implementation-gcp</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Kubernetes Implementation

```xml
<dependency>
    <groupId>io.github.makbn</groupId>
    <artifactId>mcp-mediator-implementation-kubernetes</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Docker Implementation

```xml
<dependency>
    <groupId>io.github.makbn</groupId>
    <artifactId>mcp-mediator-implementation-docker</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details. # mcp_mediator
