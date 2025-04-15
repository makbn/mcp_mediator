package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorConfigurationSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class McpMediatorConfiguration implements McpMediatorConfigurationSpec {
    String serverName;
    String serverVersion;
    boolean toolsEnabled;
    ObjectMapper serializer;

    public static McpMediatorConfigurationBuilder builder() {
        return new McpMediatorConfigurationBuilder();
    }

}
