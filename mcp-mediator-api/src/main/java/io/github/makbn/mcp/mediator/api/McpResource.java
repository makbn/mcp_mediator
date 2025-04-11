package io.github.makbn.mcp.mediator.api;

public interface McpResource<T> {

    String getName();
    String getUri();
    String getDescription();
    String getMimeType();
    T getContent();
}
