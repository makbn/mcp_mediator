package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinimalMcpMediator {
    McpMediator mediator;

    public final <T extends McpMediatorRequest<R>, R> R execute(T request) throws McpMediatorException {
        return mediator.execute(request);
    }

    public final boolean isRequestHandlerRegistered(Class<? extends McpMediatorRequestHandler<?, ?>> handlerClass) {
        return mediator.getHandlers()
                .stream().map(Object::getClass)
                .anyMatch(requestHandlerClass -> requestHandlerClass.isAssignableFrom(handlerClass));
    }
}
