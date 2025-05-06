package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.core.adaper.NativeToolAdapter;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/**
 * Mediator class responsible for managing the connection to a remote MCP server
 * and delegating tool execution requests to it.
 * <p>
 * It maintains a set of supported {@link NativeToolAdapter} instances and
 * ensures that only authorized tools are called remotely.
 *
 * @author Matt Akbarian
 */
@Getter
@ToString
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class McpMediatorRemoteMcpServer {

    /**
     * Connection object to communicate with the remote MCP server.
     */
    @NonNull
    McpSyncClient connectionToRemoteServer;

    /**
     * List of {@link NativeToolAdapter} instances supported by the remote server.
     */
    @NonNull
    List<NativeToolAdapter> toolAdapters;


    /**
     * Handles a remote tool execution request.
     * <p>
     * It verifies that the specified tool is supported, and if so,
     * forwards the request to the remote server with the given client arguments.
     *
     * @param toolAdapter     the tool adapter representing the tool to be executed
     * @param clientPassedArgs the arguments passed by the client to the proxy server for the tool execution
     * @return the result of the remote tool call
     * @throws McpMediatorException if the remote server does not support the requested tool
     */
    public McpSchema.CallToolResult handleRemoteRequest(@NonNull NativeToolAdapter toolAdapter,
                                                        @NonNull Map<String, Object> clientPassedArgs) {
        if (!toolAdapters.contains(toolAdapter)) {
            throw new McpMediatorException("invocated tool is not supported by the remote server");
        }
        return connectionToRemoteServer.callTool(new McpSchema.CallToolRequest(toolAdapter.getMethod(), clientPassedArgs));
    }
}
