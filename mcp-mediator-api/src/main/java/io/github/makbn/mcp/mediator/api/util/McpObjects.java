package io.github.makbn.mcp.mediator.api.util;


import io.github.makbn.mcp.mediator.api.McpMediatorException;

public final class McpObjects {

    private McpObjects() {
        // NO - OP
    }

    @SuppressWarnings({"unchecked", "java:S1854"})
    public static <T> T sneakyCast(Object object) {
        if (object == null) {
            return null;
        } else {
            try {
                return  (T) object;
            }catch (ClassCastException e) {
                throw new McpMediatorException("Failed to cast the result", e, McpMediatorStatus.ERROR);
            }
        }
    }
}
