package com.hikvision.hbfa.sf.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleCache<K, V>
        implements Function<K, V>,
        BiConsumer<K, V>, Consumer<K> {

    private final Map<K, V> inner = new ConcurrentHashMap<>();

    private transient Map<K, V> readonly;


    public void set(K key, V value) {
        inner.put(key, value);
    }

    public V get(K key) {
        return inner.get(key);
    }

    public void del(K key) {
        inner.remove(key);
    }

    public int size() {
        return inner.size();
    }

    public Map<K, V> all() {
        Map<K, V> a;
        if ((a = readonly) != null) return a;
        return readonly = Collections.unmodifiableMap(inner);
    }

    public V apply(K k) {
        return get(k);
    }

    public void accept(K k, V v) {
        set(k, v);
    }

    public void accept(K k) {
        del(k);
    }

}
