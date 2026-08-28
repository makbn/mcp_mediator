# MCP Mediator Dropbox Implementation

This module provides Dropbox service integration for the MCP Mediator. It leverages the official Dropbox Java SDK to expose Dropbox account and file management operations as MCP tools to LLMs.

## Exposed Tools
- `get_account_info`: Retrieve the current authenticated Dropbox account information.
- `list_folder`: List contents (files and subfolders) in a specific Dropbox path.

## Usage
Simply initialize the `DropboxMcpService` with your configured `DbxClientV2` and register it using the `McpServiceFactory`.
