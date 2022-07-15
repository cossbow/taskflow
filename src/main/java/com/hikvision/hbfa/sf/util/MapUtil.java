package com.hikvision.hbfa.sf.util;


import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;

final
public class MapUtil {
    private MapUtil() {
    }


    public static int size(Map<?, ?> m) {
        return null != m ? m.size() : 0;
    }

    public static <K, V> V get(Map<K, V> m, K key, V def) {
        return m != null ? m.getOrDefault(key, def) : def;
    }

    public static <K, V> Map<K, V> nonnull(Map<K, V> m) {
        return null == m ? Map.of() : m;
    }

    public static <K, V> Map<K, V> merge(Map<K, V> s, K key, V value) {
        var t = new HashMap<K, V>(size(s) + 1);
        if (null != s) t.putAll(s);
        t.put(key, value);
        return t;
    }

    /**
     * <h3>深度递归合并Map</h3>
     * <ol>
     *     <li>用replace的值覆盖origin，如果是嵌套的Map则递归进行合并，如果是集合则合并数据</li>
     *     <li>origin必须是可写的</li>
     * </ol>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void deepMerge(Map origin, Map replace) {
        for (Map.Entry e2 : (Set<Map.Entry>) replace.entrySet()) {
            var key = e2.getKey();
            var v2 = e2.getValue();
            var v1 = origin.get(key);
            if (v2 instanceof Map) {
                if (v1 instanceof Map) {
                    deepMerge((Map) v1, (Map) v2);
                } else {
                    origin.put(key, v2);
                }
            } else if (v2 instanceof Collection) {
                if (v1 instanceof Collection) {
                    ((Collection) v1).addAll((Collection) v2);
                } else {
                    origin.put(key, v2);
                }
            } else {
                origin.put(key, v2);
            }
        }
    }


    //

    public static Object deepCopy(Object s) {
        if (null == s) return null;
        if (s instanceof Collection) {
            return (deepCopy((Collection<?>) s));
        } else if (s instanceof Map) {
            return (deepCopy((Map<?, ?>) s));
        } else {
            return s;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <E> Collection<E> deepCopy(Collection src) {
        var dst = new ArrayList(src.size());
        for (var e : src) {
            dst.add(deepCopy(e));
        }
        return dst;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <K, V> Map<K, V> deepCopy(Map src) {
        var dst = new HashMap(src.size());
        for (var e : (Set<Map.Entry>) src.entrySet()) {
            Object key = e.getKey();
            Object value = deepCopy(e.getValue());
            dst.put(key, value);
        }
        return dst;
    }


    //

    public static <T, K, V> Map<K, V> toMap(Collection<T> c,
                                            Function<T, K> keyMap,
                                            Function<T, V> valueMap) {
        var m = new HashMap<K, V>(c.size());
        for (T t : c) {
            m.put(keyMap.apply(t), valueMap.apply(t));
        }
        return m;
    }

    public static <K, V> Map<K, V> toMap(Collection<V> c,
                                         Function<V, K> keyMap) {
        return toMap(c, keyMap, Function.identity());
    }

    public static <T, V> LongObjectMap<V> toLongMap(Collection<T> c,
                                                    ToLongFunction<T> keyMap,
                                                    Function<T, V> valueMap) {
        var m = new LongObjectHashMap<V>(c.size());
        for (T t : c) {
            m.put(keyMap.applyAsLong(t), valueMap.apply(t));
        }
        return m;
    }

    public static <V> LongObjectMap<V> toLongMap(Collection<V> c,
                                                 ToLongFunction<V> keyMap) {
        return toLongMap(c, keyMap, Function.identity());
    }


    public static <T, R> Set<R> toSet(Collection<T> c, Function<T, R> mapper) {
        var set = new HashSet<R>(c.size());
        for (T t : c) {
            set.add(mapper.apply(t));
        }
        return set;
    }


    //

    public static <K, V> Map<K, V> copyOf(Map<K, V> s) {
        if (null == s || s.isEmpty()) return Map.of();
        return Map.copyOf(s);
    }

}
