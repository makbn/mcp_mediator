package io.github.makbn.mcp.mediator.docker.request.result;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(staticName = "create")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DockerMcpResult {
    Map<String, Object> result = new HashMap<>();

    public DockerMcpResult addResult(String key, Object value) {
        result.put(key, value);
        return this;
    }
}
