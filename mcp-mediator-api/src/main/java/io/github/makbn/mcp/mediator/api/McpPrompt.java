package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.*;

/**
 * Exposes a method as an MCP prompt. Counterpart of Spring AI's {@code @McpPrompt}; both are honoured.
 * Return {@code String} (user message), {@code List<String>}, or
 * {@code io.modelcontextprotocol.spec.McpSchema.GetPromptResult}. Parameters become prompt arguments
 * (see {@link McpArg}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpPrompt {
    McpIcon[] icons() default {};

    McpClientCapability[] requiredCapabilities() default {};

    String name() default "";

    String title() default "";

    String description() default "";
}
