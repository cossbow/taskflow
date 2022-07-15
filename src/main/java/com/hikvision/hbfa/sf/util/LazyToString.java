package com.hikvision.hbfa.sf.util;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * <h3>延迟生成String类</h3>
 * <i>非并发安全</i>
 */
@JsonSerialize(using = ToStringSerializer.class)
final
public class LazyToString implements CharSequence {
    private final Supplier<String> supplier;

    private transient String str;

    private LazyToString(Supplier<String> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    private String get() {
        var s = str;
        if (null == s) {
            str = s = Objects.requireNonNull(supplier.get());
        }
        return s;
    }

    @Override
    public int length() {
        return get().length();
    }

    @Override
    public char charAt(int index) {
        return get().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return get().substring(start, end);
    }

    @Override
    public IntStream chars() {
        return get().chars();
    }

    @Override
    public IntStream codePoints() {
        return get().codePoints();
    }

    @Override
    public String toString() {
        return get();
    }


    public static LazyToString of(Supplier<String> supplier) {
        return new LazyToString(supplier);
    }

}
