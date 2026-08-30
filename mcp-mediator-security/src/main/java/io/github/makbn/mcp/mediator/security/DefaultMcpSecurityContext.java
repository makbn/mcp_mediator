package io.github.makbn.mcp.mediator.security;

public class DefaultMcpSecurityContext implements McpSecurityContext {
    private McpAuthentication authentication;

    @Override
    public McpAuthentication getAuthentication() {
        return authentication;
    }

    @Override
    public void setAuthentication(McpAuthentication authentication) {
        this.authentication = authentication;
    }
}
