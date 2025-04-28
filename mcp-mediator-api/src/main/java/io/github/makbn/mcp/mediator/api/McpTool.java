package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata annotation to map an MCP tool definition to a {@link McpMediatorRequest}. Tools are identified by unique
 * names and can include descriptions to guide their usage. Tools represent dynamic operations that can modify the state
 * or interact with external systems.
 *
 * @author Matt Akbarian
 * @see <a href="https://modelcontextprotocol.io/docs/concepts/tools">MCP Tool Concept</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@SuppressWarnings("unused")
public @interface McpTool {

    final class NotSpecified {
        private NotSpecified() {
            // NO-OP
        }
    }
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

        /**
         * If true, repeated calls with the same args have no additional effect.
         */
        boolean idempotentHint() default false;

        /**
         * If true, the tool interacts with external entities.
         */
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
    Class<?> schema() default NotSpecified.class;

    /**
     * @return Optional hints about tool behavior.
     */
    McpAnnotation[] annotations() default {};
}
