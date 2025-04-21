package io.github.makbn.mcp.mediator.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpTransportType;
import io.github.makbn.mcp.mediator.core.ProxyMcpMediator;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorConfigurationBuilder;
import io.github.makbn.mcp.mediator.core.configuration.McpMediatorProxyConfiguration;

import java.util.Arrays;
import java.util.List;


public class ProxyMcpMediatorStdioExample {
    public static final String MY_EXAMPLE_MCP_SERVER_STDIO = "my_example_proxy_mcp_server_stdio";

    public static void main(String[] args) {
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
    }
}
