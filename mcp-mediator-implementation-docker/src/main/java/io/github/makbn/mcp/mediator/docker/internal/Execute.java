package io.github.makbn.mcp.mediator.docker.internal;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.AsyncDockerCmd;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Utility class for executing asynchronous Docker commands and converting
 * callback-based responses into CompletableFuture-style synchronous results.
 *
 * @author Matt Akbarian
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Execute {

    /**
     * Enum representing the execution step within an asynchronous callback flow.
     */
    public enum ExecutionStep {
        START,
        NEXT,
        ERROR,
        COMPLETE,
        CLOSE;
    }

    /**
     * Functional interface for defining custom result handling strategies
     * for asynchronous Docker command callbacks.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface Strategy<T> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         */
        void accept(ExecutionStep step, Object t, CompletableFuture<T> future);

        /**
         * Returns a composed {@code BiConsumer} that performs, in sequence, this
         * operation followed by the {@code after} operation. If performing either
         * operation throws an exception, it is relayed to the caller of the
         * composed operation.  If performing this operation throws an exception,
         * the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         * @return a composed {@code BiConsumer} that performs in sequence this
         * operation followed by the {@code after} operation
         * @throws NullPointerException if {@code after} is null
         */
        default Strategy<T> andThen(Strategy<T> after) {
            Objects.requireNonNull(after);

            return (step, t, future) -> {
                accept(step, t, future);
                after.accept(step, t, future);
            };
        }
    }

    /**
     * Executes an asynchronous Docker command and retrieves the result using the given strategy.
     *
     * @param cmd    the asynchronous Docker command
     * @param result the strategy to handle the result and errors
     * @param <T>    the expected return type
     */

    public static <T> T getResult(AsyncDockerCmd<?, T> cmd, Strategy<T> result) throws ExecutionException,
            InterruptedException, TimeoutException {
        return getResult(cmd, Function.identity(), result);
    }


    /**
     * Executes an asynchronous Docker command with a result converter and retrieves the final result.
     *
     * @param cmd       the asynchronous Docker command
     * @param converter a function to convert the result type
     * @param result    the strategy to handle the result and errors
     * @param <T>       the return type after conversion
     * @param <M>       the raw type returned by the Docker command
     */
    public static <T, M> T getResult(AsyncDockerCmd<?, M> cmd, Function<M, T> converter, Strategy<M> result) throws ExecutionException,
            InterruptedException, TimeoutException {
        CompletableFuture<M> future = new CompletableFuture<>();

        cmd.exec(new ResultCallback<M>() {
            @Override
            public void close() {
                result.accept(ExecutionStep.CLOSE, null, future);
            }

            @Override
            public void onStart(Closeable closeable) {
                result.accept(ExecutionStep.START, closeable, future);
            }

            @Override
            public void onNext(M object) {
                result.accept(ExecutionStep.NEXT, converter.apply(object), future);
            }

            @Override
            public void onError(Throwable throwable) {
                result.accept(ExecutionStep.ERROR, throwable, future);
            }

            @Override
            public void onComplete() {
                result.accept(ExecutionStep.COMPLETE, null, future);
            }
        });

        return converter.apply(future.get(10, TimeUnit.MINUTES));
    }
}
