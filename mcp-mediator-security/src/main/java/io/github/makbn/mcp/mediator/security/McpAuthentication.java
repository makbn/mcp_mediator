package io.github.makbn.mcp.mediator.security;

import java.io.Serializable;

/**
 * Represents the token or authentication object for an MCP client.
 */
public interface McpAuthentication extends Serializable {
    /**
     * @return true if the user is authenticated
     */
    boolean isAuthenticated();

    /**
     * @return the principal/username/token string
     */
    Object getPrincipal();
}
