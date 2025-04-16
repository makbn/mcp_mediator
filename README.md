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
