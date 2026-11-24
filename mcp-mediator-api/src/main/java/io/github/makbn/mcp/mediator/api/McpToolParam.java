package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.*;

/**
 * Describes a tool method parameter. Counterpart of Spring AI's {@code @McpToolParam}; both are honoured.
 * The parameter name is the Java parameter name (compile with {@code -parameters}) unless {@code name} is given.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface McpToolParam {
    /** @return explicit argument name; empty means use the Java parameter name. */
    String name() default "";

    String description() default "";

    boolean required() default true;

    /**
     * Mirror this argument into an {@code Mcp-Param-<name>} HTTP header (schema keyword {@code x-mcp-header}); only
     * string, integer and boolean parameters qualify.
     */
    boolean header() default false;
}
