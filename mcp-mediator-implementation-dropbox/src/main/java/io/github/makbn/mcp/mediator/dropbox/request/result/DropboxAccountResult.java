package io.github.makbn.mcp.mediator.dropbox.request.result;

import com.dropbox.core.v2.users.FullAccount;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;



@Builder
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DropboxAccountResult extends AbstractDropBoxResult<FullAccount> {
    FullAccount account;

    @Override
    public FullAccount getResult() {
        return account;
    }
}
