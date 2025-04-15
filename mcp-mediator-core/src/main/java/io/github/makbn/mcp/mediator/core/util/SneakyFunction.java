package io.github.makbn.mcp.mediator.core.util;

import lombok.SneakyThrows;

import java.util.function.Function;

@FunctionalInterface
public interface SneakyFunction<T, R> extends Function<T, R> {


    R sneakyApply(T t) throws Exception;


    @Override
    @SneakyThrows
    default R apply(T t) {
        return sneakyApply(t);
    }
}
