# Security Policy

## Reporting a Vulnerability

If you believe you've found a security vulnerability, please follow these steps:

1. **Do not** disclose the vulnerability publicly.
2. Email your findings to `mehdi74akbarian [at] gmail com`.
3. Include as much information as possible about the vulnerability, including:
   - The type of vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes(I'm not a Security Engineer)

I will:
- Keep you informed about our progress in addressing the issue
- Publicly acknowledge your responsible disclosure after the vulnerability is fixed

## Security Considerations

When using the MCP Mediator, please be aware of the following security considerations:

1. **Transport Security**
   - Use secure transport mechanisms (e.g., HTTPS) when available
   - Validate all incoming connections
   - Implement proper authentication when needed

2. **Request Handling**
   - Validate all incoming requests
   - Implement proper input sanitization
   - Use appropriate access controls
   - Set MCP tool annotations properly like if the tool may perform a distructive transaction or may interacts with external entities
     - Read more: https://modelcontextprotocol.io/docs/concepts/tools#tool-annotations



> [!CAUTION]
> This project is maintained on a best-effort basis and may not receive frequent security updates or bug fixes. Users should:
>
> - Be aware that this is an experimental project
> - Understand that security vulnerabilities might not be addressed immediately
> - Use the software at their own risk
> - Consider their specific security requirements before implementing in production environments
> - Regularly monitor for updates
