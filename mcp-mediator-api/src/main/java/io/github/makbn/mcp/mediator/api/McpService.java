package io.github.makbn.mcp.mediator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link McpService} annotation is used to define an MCP (Model Context Protocol) service that provides a set
 * of tools. This annotation maps a service to a unique name, description, and allows the configuration of metadata for
 * the service's operations through associated tools, which are identified by unique names and offer specific
 * functionality.
 * <p>
 * Each service annotated with {@link McpService} can contain multiple tools, each of which is responsible for a
 * specific operation.
 * These tools can modify the system state or interact with external systems as described in the annotations on the
 * methods that implement them.
 * </p>
 * <p>
 * The tools represented in the service are intended to provide dynamic operations within the system.
 * Each tool is described in detail with input parameters, return types, and their expected behavior.
 * </p>
 *
 * @author Matt Akbarian
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpService {
    /**
     * The unique identifier for the service. This name is used to refer to the service in the broader context of
     * the MCP Mediator.
     *
     * @return the name of the service.
     */
    String name() default "";
    /**
     * A human-readable description of the service. This should explain the purpose and function of the service
     * within the MCP Mediator.
     *
     * @return the description of the service.
     */
    String description() default "";

    /**
     * Optional metadata to describe the behavior of the tools provided by the service.
     * Each tool may have specific behaviors, such as whether it is destructive, read-only, or interacts with external
     * entities.
     * These annotations can be used to provide additional insights into the tool's operation.
     *
     * @return optional hints about the tools provided by the service.
     */
    McpTool.McpAnnotation[] annotations() default {};
}
