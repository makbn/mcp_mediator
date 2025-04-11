package io.github.makbn.mcp.mediator.docker.request;

import io.github.makbn.mcp.mediator.api.McpRequest;

import java.util.Map;

/**
 * Docker request that follows the Model Context Protocol specification.
 * Represents a request to execute a Docker tool with its parameters.
 */
public class DockerRequest implements McpRequest {
    private final String tool;
    private final Map<String, Object> params;

    public DockerRequest(String tool, Map<String, Object> params) {
        this.tool = tool;
        this.params = params;
    }

    @Override
    public String getType() {
        return "docker";
    }

    @Override
    public String getImplementationName() {
        return "docker";
    }

    /**
     * Gets the tool name to execute.
     *
     * @return the tool name
     */
    public String getTool() {
        return tool;
    }

    /**
     * Gets a string parameter value.
     *
     * @param name the parameter name
     * @return the parameter value
     */
    public String getStringParam(String name) {
        Object value = params.get(name);
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("Missing required parameter: %s", name));
        }
        return value.toString();
    }

    /**
     * Gets a string array parameter value.
     *
     * @param name the parameter name
     * @return the parameter value
     */
    public String[] getStringArrayParam(String name) {
        Object value = params.get(name);
        if (value == null) {
            return new String[0];
        }
        if (value instanceof String[]) {
            return (String[]) value;
        }
        throw new IllegalArgumentException(
            String.format("Parameter %s must be a string array", name));
    }

    /**
     * Gets a boolean parameter value.
     *
     * @param name the parameter name
     * @return the parameter value
     */
    public boolean getBooleanParam(String name) {
        Object value = params.get(name);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new IllegalArgumentException(
            String.format("Parameter %s must be a boolean", name));
    }

    /**
     * Gets an integer parameter value.
     *
     * @param name the parameter name
     * @param defaultValue the default value if parameter is not present
     * @return the parameter value
     */
    public int getIntegerParam(String name, int defaultValue) {
        Object value = params.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Parameter %s must be an integer", name));
        }
    }

    /**
     * Gets a map parameter value.
     *
     * @param name the parameter name
     * @return the parameter value
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMapParam(String name) {
        Object value = params.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new IllegalArgumentException(
            String.format("Parameter %s must be a map", name));
    }
} 