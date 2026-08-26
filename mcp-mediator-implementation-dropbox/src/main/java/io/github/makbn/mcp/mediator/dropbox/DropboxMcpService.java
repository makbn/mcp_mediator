package io.github.makbn.mcp.mediator.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.users.FullAccount;
import io.github.makbn.mcp.mediator.api.McpService;
import io.github.makbn.mcp.mediator.api.McpTool;

@McpService(name = "dropbox_mcp_server")
public class DropboxMcpService {

    private final DbxClientV2 client;

    public DropboxMcpService(DbxClientV2 client) {
        this.client = client;
    }

    @McpTool(name = "get_account_info", description = "Get current Dropbox account information")
    public FullAccount getAccountInfo() throws DbxException {
        return client.users().getCurrentAccount();
    }

    @McpTool(name = "list_folder", description = "List files and folders in a Dropbox path (use empty string for root)")
    public ListFolderResult listFolder(String path) throws DbxException {
        return client.files().listFolder(path);
    }
}
