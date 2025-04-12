package io.github.makbn.mcp.mediator.dropbox.request;

import java.util.List;

public class DropboxAccountInformationRequest extends AbstractDropboxRequest {
    @Override
    public String getMethod() {
        return "get dropbox account information";
    }

    @Override
    public List<Object> getParameters() {
        return List.of();
    }
}
