package io.github.makbn.mcp.mediator.core.util;


import io.github.makbn.mcp.mediator.api.McpMediatorException;
import lombok.SneakyThrows;

import java.util.function.Function;
import java.util.function.Supplier;

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
                throw new McpMediatorException("Failed to cast the result", e);
            }
        }
    }

    @SneakyThrows
    public static <T, R> R sneakyOperation(Function<T, R> writeValueAsString, T schema) {
        return writeValueAsString.apply(schema);
    }
}
