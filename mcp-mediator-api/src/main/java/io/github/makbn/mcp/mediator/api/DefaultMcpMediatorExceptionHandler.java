package io.github.makbn.mcp.mediator.api;

public class DefaultMcpMediatorExceptionHandler implements McpMediatorExceptionHandler {
    @Override
    public String handleException(McpMediatorRequest<?> request, Throwable exception) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error executing request");
        if (request != null) {
            sb.append(" [").append(request.getClass().getSimpleName()).append("]");
        }
        sb.append(": ").append(exception.getMessage());
        return sb.toString();
    }
}
