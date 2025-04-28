package io.github.makbn.mcp.mediator.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.github.makbn.mcp.mediator.api.McpToolAdapter;
import io.github.makbn.mcp.mediator.core.internal.McpMethodArgumentDescriber;
import io.github.makbn.mcp.mediator.core.internal.McpMethodSchemaGenerator;
import io.github.makbn.mcp.mediator.core.util.McpUtils;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Adapter class for wrapping a Java {@link Method} into an {@link McpToolAdapter} interface.
 * <p>
 * This class allows standard extraction of method name, description, and input parameter schema
 * for use within the MCP mediator, either based on annotations ({@link McpTool}) or by convention.
 * </p>
 *
 * <p>
 * If a method is annotated with {@link McpTool}, its metadata will be extracted directly.
 * Otherwise, fallback mechanisms like camel-case conversion for names and automatic description
 * generation are used.
 * </p>
 *
 * @author Matt Akbarian
 */
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class McpMethodAdapter implements McpToolAdapter<Method> {
    /**
     * The executable method being adapted.
     */
    Method executableMethod;

    ObjectMapper objectMapper;

    /**
     * Optional pre-set description for the method. If null, will be dynamically extracted. As it is not possible
     * to generate high-quality human-readable descriptions for a Java method, it is recommended to provide a
     * description for each method.
     */
    @NonFinal
    @Builder.Default
    String description = null;


    /**
     * Extracts the method name used in MCP operations.
     * <p>
     * If the method is annotated with {@link McpTool}, its name is used.
     * Otherwise, the method name is converted from camelCase to snake_case.
     * </p>
     *
     * @return the method name
     */
    @NonNull
    @Override
    public String getMethod() {
        return extractMethodName();
    }

    @NonNull
    @Override
    public String getAnnotations() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns the description for the method.
     * <p>
     * If a description was pre-set, it is used. Otherwise, it is extracted
     * from the {@link McpTool} annotation or generated based on method arguments.
     * </p>
     *
     * @return the description of the method
     */
    @NonNull
    @Override
    public String getDescription() {
        return Objects.requireNonNullElse(description, extractMethodDescription());
    }

    /**
     * Generates the input parameter schema for the method.
     *
     * @return the JSON string representing the method's input schema
     * @throws McpMediatorException if schema generation fails
     */
    @NonNull
    @Override
    public String getSchema() {
        try {
            return McpMethodSchemaGenerator.of(objectMapper)
                    .generateSchemaForMethod(getSourceTool());
        } catch (JsonProcessingException e) {
            throw new McpMediatorException(String.format("Failed to extract schema for: %s", getSourceTool()), e);
        }
    }

    @NonNull
    @Override
    public Method getSourceTool() {
        return executableMethod;
    }

    @NonNull
    private String extractMethodName() {
        if (getSourceTool().isAnnotationPresent(McpTool.class)) {
            return Objects.requireNonNull(getSourceTool().getAnnotation(McpTool.class)).name();
        } else {
            return McpUtils.convertCamelCaseToSnake(getSourceTool().getName());
        }
    }

    @NonNull
    private String extractMethodDescription() {
        if (getSourceTool().isAnnotationPresent(McpTool.class)) {
            return Objects.requireNonNull(getSourceTool().getAnnotation(McpTool.class)).description();
        } else {
            return McpMethodArgumentDescriber.describeMethod(getSourceTool());
        }
    }

    @Override
    public String toString() {
        return "McpMethodAdapter{" +
                "name='" + getMethod() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", schema='" + getSchema() + '\'' +
                "}";
    }
}
