package io.github.makbn.mcp.mediator.core;

import io.github.makbn.mcp.mediator.core.internal.MinimalMcpMediator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class McpExecutionContext {
    private static final ThreadLocal<McpExecutionContext> CURRENT = new ThreadLocal<>();

    @NonNull
    MinimalMcpMediator mediator;
    @Nullable
    McpExecutionContext parent;
    @NonNull
    Map<String, Object> storage = new ConcurrentHashMap<>();


    public static McpExecutionContext get() {
        return CURRENT.get();
    }

    static void set(MinimalMcpMediator mediator, @Nullable McpExecutionContext parent) {
        CURRENT.set(McpExecutionContext.of(mediator, parent));
    }

    static void remove() {
        CURRENT.remove();
    }
}
