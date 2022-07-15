package com.hikvision.hbfa.sf.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class TypedBeanManager<E extends Typed, B extends TypedBean<E>>
        implements Function<E, B> {

    private final String name;
    private final Map<E, B> beansMap;

    protected TypedBeanManager(String name, List<B> beans) {
        this.name = name;
        this.beansMap = makeMap(beans);
    }

    public B get(E type) {
        var ch = (B) beansMap.get(type);
        if (null != ch) return ch;

        throw new IllegalArgumentException(name + " not support " + type);
    }

    public Set<E> supports() {
        return beansMap.keySet();
    }

    @Override
    public B apply(E e) {
        return get(e);
    }


    private Map<E, B> makeMap(List<B> li) {
        if (ValueUtil.isEmpty(li)) return Map.of();
        var first = li.get(0);
        var map = new HashMap<E, B>();
        for (var b : li) {
            var old = map.putIfAbsent(b.type(), b);
            if (null != old) {
                throw new IllegalStateException(first.getClass().getSimpleName() +
                        " has conflict with type " + b.type());
            }
        }
        return Map.copyOf(map);
    }
}
