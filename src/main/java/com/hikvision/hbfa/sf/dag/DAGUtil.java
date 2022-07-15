
package com.hikvision.hbfa.sf.dag;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

final
class DAGUtil {
    private DAGUtil() {
    }

    public static int sizeOf(Collection<?> c) {
        return null == c ? 0 : c.size();
    }

    public static <E, C extends Collection<E>, S extends Collection<E>>
    Set<E> subtract(C minuend, S subtraction) {
        var result = new HashSet<>(minuend);
        result.removeAll(subtraction);
        return result;
    }

    public static <Key> Function<Key, Set<Key>> hashSet() {
        return k -> new HashSet<>();
    }


    public static <Key> Map<Key, Set<Key>> toImmutable(Map<Key, Set<Key>> src) {
        var dst = new HashMap<>(src);
        for (var entry : dst.entrySet()) {
            entry.setValue(Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(dst);
    }

    public static <Key> Map.Entry<Boolean, List<Key>> topologicalSort(
            Collection<Key> keys,
            Map<Key, Set<Key>> forward,
            Map<Key, Set<Key>> reverse) {
        var result = new ArrayList<Key>(keys.size());
        var zeroInDegree = new ArrayDeque<Key>(keys.size());
        var hasInDegree = new HashMap<Key, AtomicInteger>();
        for (Key key : keys) {
            var prev = reverse.get(key);
            int size = sizeOf(prev);
            if (size == 0) {
                zeroInDegree.add(key);
                result.add(key);
            } else {
                hasInDegree.put(key, new AtomicInteger(size));
            }
        }
        if (zeroInDegree.isEmpty()) {
            return Map.entry(Boolean.FALSE, result);
        }

        while (zeroInDegree.size() > 0) {
            Key key = zeroInDegree.poll();

            var next = forward.get(key);
            if (sizeOf(next) > 0) {
                for (Key nk : next) {
                    if (hasInDegree.get(nk).decrementAndGet() == 0) {
                        result.add(nk);
                        zeroInDegree.add(nk);
                        hasInDegree.remove(nk);
                    }
                }
            }
        }

        return Map.entry(hasInDegree.isEmpty(), result);
    }

    static <Key, R> R convert(Set<Key> keys,
                              Collection<Map.Entry<Key, Key>> edges,
                              TriFunction<Set<Key>,
                                      Map<Key, Set<Key>>,
                                      Map<Key, Set<Key>>, R> mapping,
                              R failReturn) {
        var forward = new HashMap<Key, Set<Key>>(edges.size());
        var reverse = new HashMap<Key, Set<Key>>(edges.size());
        for (var edge : edges) {
            Key from = edge.getKey(), to = edge.getValue();
            if (Objects.equals(from, to)) {
                return failReturn;
            }
            forward.computeIfAbsent(from, hashSet()).add(to);
            reverse.computeIfAbsent(to, hashSet()).add(from);
        }
        return mapping.apply(Set.copyOf(keys), forward, reverse);
    }

    public static <Key> Map.Entry<Boolean, List<Key>> topologicalSort(
            Set<Key> keys,
            Collection<Map.Entry<Key, Key>> edges) {

        return convert(keys, edges, DAGUtil::topologicalSort,
                Map.entry(false, List.of()));
    }

    public static <Key> boolean checkAcyclic(
            Set<Key> keys,
            Map<Key, Set<Key>> forward,
            Map<Key, Set<Key>> reverse) {
        return topologicalSort(keys, forward, reverse).getKey();
    }

    public static <Key> boolean checkAcyclic(
            Set<Key> keys,
            Collection<Map.Entry<Key, Key>> edges) {
        return topologicalSort(keys, edges).getKey();
    }


    //

    public static <Key> void bfs(
            Set<Key> keys,
            Map<Key, Set<Key>> forward,
            Map<Key, Set<Key>> reverse,
            Consumer<Key> consumer) {
        var traveled = new HashMap<Key, Boolean>(keys.size());
        var heads = subtract(keys, reverse.keySet());
        var queue = new ArrayDeque<>(heads);
        for (Key head : heads) {
            traveled.put(head, Boolean.FALSE);
        }
        while (queue.size() > 0) {
            var ck = queue.pollLast();
            consumer.accept(ck);
            var next = forward.get(ck);
            if (null == next || next.isEmpty()) {
                // 没有后面了
            } else {
                // 发现后继
                for (Key nk : next) {
                    if (traveled.putIfAbsent(nk, Boolean.FALSE) != null) {
                        // 已经访问过
                    } else {
                        queue.addFirst(nk);
                    }
                }
            }
            traveled.put(ck, Boolean.TRUE);
        }

    }

    public static <Key> void bfs(
            Set<Key> keys,
            Collection<Map.Entry<Key, Key>> edges,
            Consumer<Key> consumer) {
        convert(keys, edges, (keySet, forward, reverse) -> {
            bfs(keySet, forward, reverse, consumer);
            return null;
        }, null);
    }

}
