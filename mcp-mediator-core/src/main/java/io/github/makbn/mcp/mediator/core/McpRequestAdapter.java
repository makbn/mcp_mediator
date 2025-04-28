package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.github.makbn.mcp.mediator.api.McpToolAdapter;
import io.github.makbn.mcp.mediator.core.util.McpUtils;
import io.github.makbn.mcp.mediator.core.util.SneakyFunction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Objects;
import java.util.Optional;


/**
 * Adapter class for processing {@link McpTool} and extracting metadata and schema information.
 * <p>
 * This class wraps around a {@link McpMediatorRequest} implementation and uses annotations
 * like {@link McpTool} to extract method names, descriptions, and schema definitions
 * for use in the MCP system.
 * </p>
 *
 * @author Matt Akbarian
 */
@Builder
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class McpRequestAdapter implements McpToolAdapter<McpTool> {
    private static final String ERROR = "McpRequest should be annotated with '@%s'";

    Class<? extends McpMediatorRequest<?>> request;
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator schemaGenerator = new JsonSchemaGenerator(objectMapper);

    @NonNull
    @Override
    public McpTool getSourceTool() {
        return Objects.requireNonNullElseGet(request.getDeclaredAnnotation(McpTool.class), () -> {
            throw new McpMediatorException(String.format(ERROR, McpTool.class.getSimpleName()));
        });
    }

    /**
     * Gets the type of the request.
     *
     * @return the request type.
     * @throws McpMediatorException if the {@code McpTool} annotation is not present.
     */
    @NonNull
    @Override
    public String getMethod() {
        return getSourceTool().name();
    }

    /**
     * Gets the additional information about the tool being provided to the MCP client.
     *
     * @throws UnsupportedOperationException always, as this method is not yet implemented.
     */
    @NonNull
    @Override
    public String getAnnotations() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Retrieves the description of the request tool as defined by the {@link McpTool} annotation.
     * <p>
     * This description provides context or documentation for the tool associated with the request.
     * </p>
     *
     * @return the description of the tool
     * @throws McpMediatorException if the {@code McpTool} annotation is not present.
     */
    @NonNull
    @Override
    public String getDescription() {
        return getSourceTool().description();
    }

    /**
     * Converts the tool input parameter type as the MCP Server schema. See {@link McpTool#schema()}.
     *
     * @return the JSON string of the input schema.
     * @throws McpMediatorException if the {@code McpTool} annotation is not present.
     */
    @NonNull
    @Override
    public String getSchema() {
        return Optional.of(getSourceTool())
                .map(McpTool::schema)
                .map(schema -> McpUtils.sneakyOperation(
                        (SneakyFunction<Class<?>, JsonSchema>) schemaGenerator::generateSchema, schema))
                .map(schema -> McpUtils.sneakyOperation(
                        (SneakyFunction<JsonSchema, String>) objectMapper::writeValueAsString, schema))
                .orElseThrow(() -> new McpMediatorException(String.format(ERROR, McpTool.class.getSimpleName())));
    }
}
