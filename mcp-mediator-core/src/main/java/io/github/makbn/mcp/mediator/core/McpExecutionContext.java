package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.core.internal.MinimalMcpMediator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the execution context for a request being processed by the
 * {@link io.github.makbn.mcp.mediator.api.McpMediator}.
 * <p>
 * This context is thread-local and provides access to a {@link MinimalMcpMediator} and serializer
 * so that handlers can perform additional operations during request execution while respecting encapsulation.
 * <p>
 * It can also store transient data in a thread-safe key-value map during request execution.
 *
 * @author Matt Akbarian
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class McpExecutionContext {
    private static final ThreadLocal<McpExecutionContext> CURRENT = new ThreadLocal<>();

    @NonNull
    MinimalMcpMediator mediator;
    @NonNull
    ObjectMapper serializer;
    @Nullable
    McpExecutionContext parent;
    @NonNull
    Map<String, Object> storage = new ConcurrentHashMap<>();

    /**
     * Returns the current execution context for the calling thread.
     *
     * @return the current {@link McpExecutionContext}, or {@code null} if none is set
     */
    public static McpExecutionContext get() {
        return CURRENT.get();
    }

    static void set(@NonNull MinimalMcpMediator mediator, @NonNull ObjectMapper serializer, @Nullable McpExecutionContext parent) {
        CURRENT.set(McpExecutionContext.of(mediator, serializer, parent));
    }

    static void remove() {
        CURRENT.remove();
    }
}
