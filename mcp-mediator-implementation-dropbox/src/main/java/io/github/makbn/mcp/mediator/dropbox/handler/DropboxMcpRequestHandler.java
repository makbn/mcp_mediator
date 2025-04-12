package io.github.makbn.mcp.mediator.dropbox.handler;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import io.github.makbn.mcp.mediator.api.*;
import io.github.makbn.mcp.mediator.dropbox.request.AbstractDropboxRequest;
import io.github.makbn.mcp.mediator.dropbox.request.DropboxAccountInformationRequest;
import io.github.makbn.mcp.mediator.dropbox.request.result.AbstractDropBoxResult;
import io.github.makbn.mcp.mediator.dropbox.request.result.DropboxAccountResult;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DropboxMcpRequestHandler implements McpRequestHandler<AbstractDropboxRequest, AbstractDropBoxResult<?>> {
    private static final String REQUEST_HANDLER_NAME = "dropbox-mcp-request-handler";
    private static final String ACCESS_TOKEN_PARAM = "mcp.mediator.implementation.dropbox.access-token";

    DbxClientV2 client;

    public DropboxMcpRequestHandler() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder(REQUEST_HANDLER_NAME).build();
        this.client = new DbxClientV2(config, getProperties().getProperty(ACCESS_TOKEN_PARAM));
    }

    @Override
    public String getName() {
        return REQUEST_HANDLER_NAME;
    }

    @Override
    public AbstractDropBoxResult<?> handle(AbstractDropboxRequest request) throws McpMediatorException {
        try {
            if (request instanceof DropboxAccountInformationRequest) {
                return DropboxAccountResult.builder()
                        .account(client.users().getCurrentAccount())
                        .build();
            }
            return null;
        }catch (DbxException e) {
            throw  new McpMediatorException(REQUEST_HANDLER_NAME, e, McpMediatorStatus.ERROR);
        }
    }

    @Override
    public boolean canHandle(McpRequest request) {
        return request instanceof AbstractDropboxRequest;
    }



}
