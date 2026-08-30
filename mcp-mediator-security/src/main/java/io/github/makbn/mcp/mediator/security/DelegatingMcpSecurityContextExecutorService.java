package io.github.makbn.mcp.mediator.security;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Wraps an existing ExecutorService to propagate the McpSecurityContextHolder to the background threads.
 */
public class DelegatingMcpSecurityContextExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    public DelegatingMcpSecurityContextExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    private Runnable wrap(Runnable delegateRunnable) {
        final McpSecurityContext context = McpSecurityContextHolder.getContext();
        return () -> {
            McpSecurityContextHolder.setContext(context);
            try {
                delegateRunnable.run();
            } finally {
                McpSecurityContextHolder.clearContext();
            }
        };
    }

    private <T> Callable<T> wrap(Callable<T> delegateCallable) {
        final McpSecurityContext context = McpSecurityContextHolder.getContext();
        return () -> {
            McpSecurityContextHolder.setContext(context);
            try {
                return delegateCallable.call();
            } finally {
                McpSecurityContextHolder.clearContext();
            }
        };
    }

    private <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(this::wrap).collect(Collectors.toList());
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrap(command));
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrap(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrap(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrap(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrap(tasks), timeout, unit);
    }
}
