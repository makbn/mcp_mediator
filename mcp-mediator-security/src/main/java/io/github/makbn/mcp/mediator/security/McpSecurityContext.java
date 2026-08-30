package io.github.makbn.mcp.mediator.security;

import java.io.Serializable;

/**
 * Interface defining the minimum security information associated with the current thread of execution.
 */
public interface McpSecurityContext extends Serializable {
    McpAuthentication getAuthentication();
    void setAuthentication(McpAuthentication authentication);
}
