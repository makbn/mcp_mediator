package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpToolAdapter;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.IOException;

@Builder
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NativeToolAdapter implements McpToolAdapter<McpSchema.Tool> {
    @NonNull
    McpSchema.Tool nativeTool;
    @NonNull
    ObjectMapper objectMapper;

    @NonNull
    @Override
    public McpSchema.Tool getSourceTool() {
        return nativeTool;
    }

    /**
     * Gets the type of the request.
     *
     * @return the request type.
     */
    @NonNull
    public String getMethod() {
        return getSourceTool().name();
    }

    /**
     * Gets the additional information about the tool being provided to the MCP client.
     *
     * @throws UnsupportedOperationException always, as this method is not yet implemented.
     */
    @NonNull
    public String getAnnotations() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Retrieves the description of the request tool.
     * <p>
     * This description provides context or documentation for the tool associated with the request.
     * </p>
     *
     * @return the description of the tool
     */
    @NonNull
    public String getDescription() {
        return getSourceTool().description();
    }

    /**
     * Converts the tool input parameter type as the MCP Server schema.
     *
     * @return the JSON string of the input schema.
     */
    @NonNull
    public String getSchema() {
        try {
            return objectMapper.writeValueAsString(getSourceTool().inputSchema());
        } catch (IOException e) {
            throw new McpMediatorException("Failed to convert input schema to JSON string", e);
        }

    }
}
