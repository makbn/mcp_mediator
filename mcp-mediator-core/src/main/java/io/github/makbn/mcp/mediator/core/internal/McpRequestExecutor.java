package io.github.makbn.mcp.mediator.core.internal;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.concurrent.Callable;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class McpRequestExecutor<T> implements Callable<T> {

}
