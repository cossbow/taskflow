package com.hikvision.hbfa.sf.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
public class RetryFuture<T> extends CompletableFuture<T>
        implements BiConsumer<T, Throwable>, Runnable {

    private final Supplier<CompletableFuture<T>> source;
    private final int retryTimes;
    private final Executor executor;
    private final Predicate<? super Throwable> errorFilter;

    private volatile CompletableFuture<T> sourceFuture;
    private volatile int times = 0;

    public RetryFuture(Supplier<CompletableFuture<T>> source,
                       int retryTimes, Duration retryDelay,
                       Predicate<? super Throwable> errorFilter) {
        this.errorFilter = errorFilter;
        if (retryTimes < 0) {
            throw new IllegalArgumentException("retryTimes cannot be negative");
        }
        this.source = Objects.requireNonNull(source);
        this.retryTimes = retryTimes;
        this.executor = (null == retryDelay || retryDelay.isZero()) ?
                defaultExecutor() :
                delayedExecutor(retryDelay.toNanos(), TimeUnit.NANOSECONDS);
    }

    //

    public int retryTimes() {
        return Math.max(0, times - 1);
    }

    private synchronized int incrRetries() {
        return ++times;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        var sf = sourceFuture;
        sourceFuture = null;
        if (null != sf) {
            sf.cancel(mayInterruptIfRunning);
        }
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public void accept(T t, Throwable ex) {
        if (ex == null) {
            complete(t);
            return;
        }

        if (ex instanceof CompletionException) {
            ex = ex.getCause();
        }
        if (null == errorFilter || errorFilter.test(ex)) {
            int retries = incrRetries();
            if (retries <= retryTimes) {
                log.warn("error retry {}: {}", retries, ex.getMessage());
                executor.execute(this);
                return;
            }
        }
        log.error("error retry out", ex);
        completeExceptionally(ex);
    }

    @Override
    public void run() {
        if (isDone()) {
            return;
        }

        try {
            sourceFuture = source.get();
            sourceFuture.whenComplete(this);
        } catch (Throwable e) {
            accept(null, e);
        }
    }


    //

    public static class Builder<T> {
        private Supplier<CompletableFuture<T>> src;
        private int retries;
        private Duration delay;
        private Predicate<? super Throwable> on;

        public Builder<T> src(Supplier<CompletableFuture<T>> src) {
            this.src = src;
            return this;
        }

        public Builder<T> retries(int retries) {
            this.retries = retries;
            return this;
        }

        public Builder<T> delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        public Builder<T> delay(long delay, TimeUnit timeUnit) {
            this.delay = Duration.of(delay, timeUnit.toChronoUnit());
            return this;
        }

        public Builder<T> on(Predicate<? super Throwable> on) {
            this.on = on;
            return this;
        }

        public Builder<T> on(Class<? extends Throwable> type) {
            this.on = type::isInstance;
            return this;
        }

        public RetryFuture<T> build() {
            var f = new RetryFuture<>(src, retries, delay, on);
            f.run();
            return f;
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> Builder<T> builder(Supplier<CompletableFuture<T>> src) {
        return new Builder<T>().src(src);
    }

}
