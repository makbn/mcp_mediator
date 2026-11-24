package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.*;

/**
 * Exposes a method as an MCP resource (or resource template when {@code uri} contains {@code {variables}}).
 * Counterpart of Spring AI's {@code @McpResource}; both are honoured.
 * <p>The method may return a {@code String} (text), {@code byte[]} (blob), or an
 * {@code io.modelcontextprotocol.spec.McpSchema.ReadResourceResult}; URI template variables are bound to
 * same-named parameters.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpResource {
    McpIcon[] icons() default {};

    long size() default -1;

    /** Intended audience ({@code "USER"}, {@code "ASSISTANT"}); empty = unspecified. */
    String[] audience() default {};

    /** Importance 0.0-1.0; negative = unspecified. */
    double priority() default -1;

    /** ISO-8601 last-modified timestamp; empty = unspecified. */
    String lastModified() default "";

    /** Cache time-to-live in ms for the modern server's read result; 0 = none. */
    long ttlMs() default 0;

    /** {@code "PUBLIC"} or {@code "PRIVATE"} (modern server only). */
    String cacheScope() default "PRIVATE";

    McpClientCapability[] requiredCapabilities() default {};

    String name() default "";

    String title() default "";

    String uri();

    String description() default "";

    String mimeType() default "text/plain";
}
