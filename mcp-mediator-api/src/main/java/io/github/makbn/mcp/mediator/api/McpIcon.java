package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.*;

/** Icon of a tool, resource or prompt ({@code icons = @McpIcon(src = "...")}). Counterpart of Spring AI's {@code @McpIcon}. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface McpIcon {
    String src();

    String mimeType() default "";

    String[] sizes() default {};

    String theme() default "";
}
