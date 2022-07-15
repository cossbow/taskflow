package com.hikvision.hbfa.sf.util;

import com.fasterxml.jackson.annotation.JsonValue;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


public abstract class Typed {

    private static final
    Map<Class<? extends Typed>, Map<String, ? extends Typed>> CacheMap
            = new ConcurrentHashMap<>();

    //

    private final String name;

    protected Typed(String name) {
        name = Objects.requireNonNull(name, "name").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                if (isCharacter(c)) {
                    continue;
                }
            } else {
                if (isCharacter(c) || isNumber(c) || isUnderscore(c)) {
                    continue;
                }
            }
            throw new IllegalArgumentException(
                    "name require start with char and compose by char or digit: " + name);
        }

        this.name = new String(name.getBytes(), StandardCharsets.US_ASCII);
    }

    @JsonValue
    public final String name() {
        return name;
    }

    //

    @SuppressWarnings("unchecked")
    protected static <T extends Typed> Map<String, T> cacheOf(Class<T> cls) {
        return (Map<String, T>) CacheMap.computeIfAbsent(cls, c -> new ConcurrentHashMap<>());
    }

    /**
     * 缓存起来，常驻内存
     */
    protected static <T extends Typed> T
    getOrNew0(Class<T> cls, String name, Function<String, T> creator) {
        Objects.requireNonNull(cls);
        Objects.requireNonNull(name);
        Objects.requireNonNull(creator);

        var cache = cacheOf(cls);
        return cache.computeIfAbsent(name, creator);
    }

    public static <T extends Typed> T
    valueOf(Class<T> cls, String name) {
        Objects.requireNonNull(cls);
        Objects.requireNonNull(name);

        var cache = cacheOf(cls);
        var value = cache.get(name);
        if (null != value) return value;

        throw new IllegalArgumentException(
                "No Typed constant " + cls.getCanonicalName() + "." + name);
    }

    public static <T extends Typed> Collection<T>
    values(Class<T> cls) {
        Objects.requireNonNull(cls);

        var cache = cacheOf(cls);
        return Collections.unmodifiableCollection(cache.values());
    }

    //

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var enumType = (Typed) o;
        return name.equals(enumType.name);
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    @Override
    public final String toString() {
        return name;
    }

    //

    private static boolean isCharacter(char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    private static boolean isNumber(char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isUnderscore(char c) {
        return '_' == c;
    }

}
