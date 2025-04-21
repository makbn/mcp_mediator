package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.core.NativeToolAdapter;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Getter
@ToString
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class McpMediatorRemoteMcpServer {
    @NonNull
    McpSyncClient connectionToRemoteServer;
    @NonNull
    List<NativeToolAdapter> toolAdapters;


    public McpSchema.CallToolResult handleRemoteRequest(@NonNull NativeToolAdapter toolAdapter,
                                                        @NonNull Map<String, Object> clientPassedArgs) {
        if (!toolAdapters.contains(toolAdapter)) {
            throw new McpMediatorException("invocated tool is not supported by the remote server");
        }
        return connectionToRemoteServer.callTool(new McpSchema.CallToolRequest(toolAdapter.getMethod(), clientPassedArgs));
    }
}
