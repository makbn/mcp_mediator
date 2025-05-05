package io.github.makbn.mcp.mediator.core.internal;

import io.github.makbn.mcp.mediator.api.McpMediator;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * A restricted interface to the {@link McpMediator} that exposes only the ability to execute requests
 * and verify handler registrations.
 * <p>
 * This class is intended to be used within {@link io.github.makbn.mcp.mediator.core.McpExecutionContext} to provide
 * limited mediator access to request handlers. It prevents misuse by restricting access to the full functionality
 * of the underlying {@code McpMediator}.
 *
 * @author Matt Akbarian
 */
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinimalMcpMediator {
    McpMediator mediator;

    /**
     * Executes the given {@link McpMediatorRequest} using the encapsulated {@link McpMediator}.
     *
     * @param request the mediator requests to execute
     * @param <T>     the type of the request
     * @param <R>     the expected response type
     * @return the result of executing the request
     * @throws McpMediatorException if the request execution fails
     */
    public final <T extends McpMediatorRequest<R>, R> R execute(T request) throws McpMediatorException {
        return mediator.execute(request);
    }

    /**
     * Checks whether the specified handler class is registered in the underlying mediator.
     *
     * @param handlerClass the handler class to check
     * @return {@code true} if the handler class is registered, {@code false} otherwise
     */
    public final boolean isRequestHandlerRegistered(Class<? extends McpMediatorRequestHandler<?, ?>> handlerClass) {
        return mediator.getHandlers()
                .stream().map(Object::getClass)
                .anyMatch(requestHandlerClass -> requestHandlerClass.isAssignableFrom(handlerClass));
    }
}
