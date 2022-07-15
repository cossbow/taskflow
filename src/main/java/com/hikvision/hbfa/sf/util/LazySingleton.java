package com.hikvision.hbfa.sf.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 懒加载的单例封装
 * 支持线程安全
 */
final
public class LazySingleton<T> implements Supplier<T> {

    /**
     * 创建对象的lambda，不能返回null
     */
    private final Supplier<T> creator;

    private final Predicate<T> valid;

    private final Consumer<T> onCreated;

    private volatile T instance;

    private LazySingleton(Supplier<T> creator, Predicate<T> valid, Consumer<T> onCreated) {
        this.creator = Objects.requireNonNull(creator);
        this.valid = valid;
        this.onCreated = onCreated;
    }

    private boolean invalid(T instance) {
        return null != valid && !valid.test(instance);
    }

    /**
     * 获取或创建实例
     */
    @Override
    public T get() {
        T ins = instance;
        if (null == ins || invalid(ins)) {
            synchronized (this) {
                ins = instance;
                if (null == ins || invalid(ins)) {
                    ins = creator.get();
                    if (null == ins) {
                        throw new IllegalStateException("create null instance");
                    }
                    if (invalid(ins)) {
                        throw new IllegalStateException("create closed instance");
                    }
                    if (null != onCreated) onCreated.accept(ins);
                    instance = ins;
                }
            }
        }
        return ins;
    }

    /**
     * 移除已创建的实例
     */
    public T remove() {
        T ins = instance;
        instance = null;
        return ins;
    }

    //

    /**
     * @see #of(Supplier, Predicate, Consumer)
     */
    public static <T> LazySingleton<T> of(Supplier<T> creator) {
        return of(creator, null, null);
    }

    /**
     * @see #of(Supplier, Predicate, Consumer)
     */
    public static <T> LazySingleton<T> of(Supplier<T> creator, Predicate<T> valid) {
        return of(creator, valid, null);
    }

    public static <T> LazySingleton<T> of(Supplier<T> creator, Consumer<T> onCreated) {
        return of(creator, null, onCreated);
    }

    /**
     * @param creator   延迟创建对象的方法，返回值不能为空，且创建之后会调用valid校验。
     * @param valid     校验对象是否有效，{@link #get}时会调用此方法来校验，无效则会被移除然后重新创建。
     * @param onCreated 每次调用创建方法creator之后执行的回调。
     */
    public static <T> LazySingleton<T> of(Supplier<T> creator, Predicate<T> valid, Consumer<T> onCreated) {
        return new LazySingleton<>(creator, valid, onCreated);
    }

}
