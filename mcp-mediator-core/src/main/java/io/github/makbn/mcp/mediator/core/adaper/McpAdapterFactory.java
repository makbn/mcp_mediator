package io.github.makbn.mcp.mediator.core.adaper;

import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import io.github.makbn.mcp.mediator.api.McpToolAdapter;
import io.github.makbn.mcp.mediator.core.McpServiceFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.util.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpAdapterFactory {

    @NonNull
    @SuppressWarnings("java:S1452")
    public static Collection<? extends McpToolAdapter<?>> createAdapter(
            @NonNull Class<? extends McpMediatorRequest<?>> request, @NonNull McpMediatorRequestHandler<?, ?> handler) {
        if (handler instanceof McpServiceFactory.McpServiceRequestHandler serviceRequestHandler) {
            return serviceRequestHandler.getAdapterMap()
                    .entrySet()
                    .stream()
                    .filter(entry -> request.isAssignableFrom(entry.getKey().getClass()))
                    .map(Map.Entry::getValue).toList();
        } else {
            return Collections.singleton(McpRequestAdapter.builder().request(request).build());
        }
    }
}
