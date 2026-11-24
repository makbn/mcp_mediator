package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.*;

/**
 * Marks a method that handles a server-to-client interaction when the mediator acts as an MCP client (proxy).
 * Counterpart of Spring AI's {@code @McpSampling}, {@code @McpElicitation}, {@code @McpLogging}, {@code @McpProgress}
 * and {@code @Mcp*ListChanged}; both are honoured. The method takes the request/notification as its single parameter
 * (for list-changed: the new {@code List}) and, except for notifications, returns the result.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpClientHandler {
    enum Kind { SAMPLING, ELICITATION, LOGGING, PROGRESS, TOOLS_CHANGED, RESOURCES_CHANGED, PROMPTS_CHANGED }

    Kind value();

    /** Connection names this handler applies to; empty = all connections. */
    String[] clients() default {};
}
