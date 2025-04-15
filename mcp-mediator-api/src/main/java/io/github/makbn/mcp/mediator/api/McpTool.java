package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata annotation to map an MCP tool definition to a {@link McpMediatorRequest}. Tools are identified by unique
 * names and can include descriptions to guide their usage. Tools represent dynamic operations that can modify state
 * or interact with external systems.
 *
 * @author Matt Akbarian
 * @see <a href="https://modelcontextprotocol.io/docs/concepts/tools">MCP Tool Concept</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpTool {

    /**
     * Optional hints about tool behavior.
     */
    @interface McpAnnotation {
        /**
         * @return Human-readable title for the tool.
         */
        String title() default "";

        /**
         * If true, the tool does not modify its environment.
         */
        boolean readOnlyHint() default false;

        /**
         * If true, the tool may perform destructive updates.
         */
        boolean destructiveHint() default false;

        boolean idempotentHint() default false;

        boolean openWorldHint() default false;
    }

    /**
     * @return Unique identifier for the tool.
     */
    String name() default "";

    /**
     * @return Human-readable description.
     */
    String description() default "";

    /**
     * @return JSON Schema for the tool's parameters.
     */
    Class<?> schema();

    /**
     * @return Optional hints about tool behavior
     */
    McpAnnotation[] annotations() default {};
}
