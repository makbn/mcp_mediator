package io.github.makbn.mcp.mediator.commons.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Base class for handling {@link McpMediatorRequest}s by executing a system command or running a process.
 *
 * @author Matt Akbarian
 */
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract class McpMediatorProcExecRequestHandler<T extends McpMediatorRequest<R>, R> implements McpMediatorRequestHandler<T, R> {

    ObjectMapper objectMapper;
    Executor commandExecutor;

    protected McpMediatorProcExecRequestHandler() {
        this(JsonMapper.builder().build(), DefaultExecutor.builder().get());
    }

    @Override
    public R handle(T request) throws McpMediatorException {
        return executeCommand(request);
    }

    protected R executeCommand(@NonNull T request) throws McpMediatorException {
        try {
            CommandLine cmdLine = getCommandLine(request);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);

            commandExecutor.setStreamHandler(streamHandler);

            getWorkingDirectory(request).ifPresent(commandExecutor::setWorkingDirectory);

            int exitCode = commandExecutor.execute(cmdLine, getEnvironment(request));

            String output = outputStream.toString().trim();
            String error = errorStream.toString().trim();

            return convertResult(exitCode, output, error, request);
        } catch (IOException e) {
            throw new McpMediatorException("Command execution failed", e);
        }
    }

    /**
     * Required to construct the command to run
     */
    @NonNull
    protected abstract CommandLine getCommandLine(@NonNull T request);

    /**
     * Optionally provide environment variables
     */
    @NonNull
    protected Map<String, String> getEnvironment(@NonNull T request) {
        return Collections.emptyMap();
    }

    /**
     * Optionally set a working directory
     */
    @NonNull
    protected Optional<File> getWorkingDirectory(@NonNull T request) {
        return Optional.empty();
    }

    /**
     * Convert the raw output into a usable result
     */
    @NonNull
    protected abstract R convertResult(int exitCode, String output, String error, @NonNull T request);
}
