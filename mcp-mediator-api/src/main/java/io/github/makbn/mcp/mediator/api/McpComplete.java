package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.*;

/**
 * Provides argument auto-completion for a prompt ({@code prompt}) or resource template ({@code uri}).
 * Exactly one of them must be set. The method takes the partial value as a {@code String} (or
 * {@code McpSchema.CompleteRequest.CompleteArgument}) and returns {@code List<String>},
 * {@code String}, or {@code McpSchema.CompleteResult}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpComplete {
    String prompt() default "";

    String uri() default "";
}
