package com.hikvision.hbfa.sf.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final
public class FutureUtil {
    private FutureUtil() {
    }

    public static final CompletableFuture<?>[] EMPTY = new CompletableFuture[0];

    public static <T> Consumer<T> doNothing() {
        return t -> {
        };
    }

    public
    static CompletableFuture<Void> allOf(Collection<? extends CompletableFuture<?>> futures) {
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        if (1 == futures.size()) {
            var f = futures.iterator().next();
            return f.thenAccept(v -> {
            });
        }
        return CompletableFuture.allOf(futures.toArray(EMPTY));
    }

    public
    static <S, F extends CompletableFuture<S>, C extends Collection<F>, T>
    CompletableFuture<T> allOf(C futures, Function<Void, T> supplier) {
        return allOf(futures).thenApply(supplier);
    }


    public
    static <T, F extends CompletableFuture<T>, C extends Collection<F>, R, A>
    CompletableFuture<A> collect(C src, Collector<T, R, A> collector) {
        var futures = new ArrayList<CompletableFuture<Void>>(src.size());
        var builder = Stream.<T>builder();
        for (var s : src) {
            futures.add(s.thenAccept((t) -> {
                synchronized (builder) {
                    builder.add(t);
                }
            }));
        }
        return allOf(futures, v -> {
            synchronized (builder) {
                return builder.build().collect(collector);
            }
        });
    }


    public
    static CompletableFuture<Void>
    runConcurrent(List<Runnable> tasks, Executor executor) {
        var futures = tasks.stream()
                .map(r -> CompletableFuture.runAsync(r, executor))
                .collect(Collectors.toList());
        return allOf(futures);
    }

}
