package com.hikvision.hbfa.sf.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * 带过期时间的缓存
 */
@Slf4j
public class ExpiringCache<K extends Comparable<K>, V> {

    // 毫秒
    private final long defaultExpires;

    private final ConcurrentMap<K, ValueWrap<K, V>> inner
            = new ConcurrentSkipListMap<>();

    private final AtomicInteger maxSize = new AtomicInteger();

    public ExpiringCache(long defaultExpires) {
        this.defaultExpires = defaultExpires;
        if (this.defaultExpires <= 0) {
            throw new IllegalArgumentException("require expires>0");
        }
    }


    private CompletableFuture<Map.Entry<K, V>> addExpiring(K k, V v, long expires) {
        return CompletableFuture.supplyAsync(() -> {
            inner.remove(k);
            return Map.entry(k, v);
        }, CompletableFuture.delayedExecutor(
                expires, TimeUnit.MILLISECONDS));
    }

    /**
     * 设置一个会过期的key
     *
     * @param key     缓存的key
     * @param value   缓存的value
     * @param expires 过期时间（毫秒）
     * @return 过期事件的 {@link CompletableFuture Future}
     */
    public CompletableFuture<Map.Entry<K, V>> set(K key, V value, long expires) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (expires <= 0) {
            expires = defaultExpires;
        }

        var expiredAt = System.currentTimeMillis() + expires;
        var ef = addExpiring(key, value, expires);
        var w = inner.put(key, new ValueWrap<>(value, ef, expiredAt));
        if (null != w) {
            log.error("Key-{} conflict", key);
            w.cancel();
        }
        ef.defaultExecutor().execute(() -> {
            synchronized (maxSize) {
                int size = inner.size();
                if (size > maxSize.get())
                    maxSize.set(size);
            }
        });
        return ef;
    }


    /**
     * 设置一个会过期的key，使用默认过期时间{@link #defaultExpires}
     *
     * @see #set(K, V, long)
     */
    public CompletableFuture<Map.Entry<K, V>> set(K key, V value) {
        return set(key, value, defaultExpires);
    }

    public V get(K key) {
        Objects.requireNonNull(key);

        var w = inner.computeIfPresent(key, (k, _w) -> {
            if (_w.expired()) {
                return null;
            }
            return _w;
        });
        return null == w ? null : w.value;
    }

    public V del(K key) {
        Objects.requireNonNull(key);

        var w = inner.remove(key);
        if (null == w) {
            return null;
        }
        w.cancel();
        return w.value;
    }

    public int size() {
        return inner.size();
    }

    public int maxSize() {
        return maxSize.get();
    }

    public <R> Stream<R> stream(BiFunction<K, V, R> mapper) {
        return inner.entrySet().stream()
                .map(e -> mapper.apply(e.getKey(), e.getValue().value));
    }


    //

    private static class ValueWrap<K, V> {
        private final V value;
        private final CompletableFuture<Map.Entry<K, V>> ef;
        private final long expiredAt;

        ValueWrap(V value, CompletableFuture<Map.Entry<K, V>> ef, long expiredAt) {
            this.value = Objects.requireNonNull(value);
            this.ef = Objects.requireNonNull(ef);
            this.expiredAt = expiredAt;
        }

        boolean expired() {
            return this.expiredAt > 0 && this.expiredAt <= System.currentTimeMillis();
        }

        void cancel() {
            ef.cancel(false);
        }
    }

}
