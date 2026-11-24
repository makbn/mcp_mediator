package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.*;

/** Describes a prompt argument. Counterpart of Spring AI's {@code @McpArg}; both are honoured. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface McpArg {
    String name() default "";

    String title() default "";

    String description() default "";

    boolean required() default true;
}
