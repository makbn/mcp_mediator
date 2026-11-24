package io.github.makbn.mcp.mediator.api;

import java.util.Map;

/** Request {@code _meta} of the current call; declare it as a tool parameter and it is injected (not in the schema). Also accepts Spring AI's {@code McpMeta}. */
public record McpMeta(Map<String, Object> meta) {
    public Object get(String key) {
        return meta == null ? null : meta.get(key);
    }
}
