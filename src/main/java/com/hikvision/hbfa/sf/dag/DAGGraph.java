package com.hikvision.hbfa.sf.dag;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * <h3>有向无环图</h3>
 * <div>设计为不可变类型，创建时构建好图并检查错误</div>
 * <div>2021-12-14</div>
 *
 * @author jiangjianjun5
 */
final
public class DAGGraph<Key> {

    private final Set<Key> allNodes;

    private final Set<Key> heads, tails;

    private final Map<Key, Set<Key>> forwardIndex;
    private final Map<Key, Set<Key>> reverseIndex;

    public DAGGraph(Collection<Key> allNodes,
                    Iterable<Map.Entry<Key, Key>> edges) {
        if (null == allNodes || allNodes.isEmpty()) {
            throw new IllegalArgumentException("keys empty");
        }
        this.allNodes = Set.copyOf(allNodes);
        if (this.allNodes.size() != allNodes.size()) {
            throw new IllegalArgumentException("Has duplicate Key");
        }

        var keyMap = new HashMap<Key, Key>(this.allNodes.size());
        for (Key k : this.allNodes) {
            keyMap.put(k, k);
        }

        var forward = new HashMap<Key, Set<Key>>();
        var reverse = new HashMap<Key, Set<Key>>();
        for (var edge : edges) {
            Key from = keyMap.get(edge.getKey()), to = keyMap.get(edge.getValue());
            if (from == null) {
                throw new IllegalArgumentException("Key not exists: " + edge.getKey());
            }
            if (to == null) {
                throw new IllegalArgumentException("Key not exists: " + edge.getValue());
            }

            forward.computeIfAbsent(from, DAGUtil.hashSet()).add(to);
            reverse.computeIfAbsent(to, DAGUtil.hashSet()).add(from);
        }
        if (!DAGUtil.checkAcyclic(this.allNodes, forward, reverse)) {
            throw new IllegalArgumentException("Serious error: graph has cycle！");
        }

        this.forwardIndex = DAGUtil.toImmutable(forward);
        this.reverseIndex = DAGUtil.toImmutable(reverse);
        this.tails = Set.copyOf(DAGUtil.subtract(this.allNodes, this.forwardIndex.keySet()));
        this.heads = Set.copyOf(DAGUtil.subtract(this.allNodes, this.reverseIndex.keySet()));
    }


    //

    public Set<Key> allNodes() {
        return allNodes;
    }

    public Set<Key> heads() {
        return heads;
    }

    public Set<Key> tails() {
        return tails;
    }

    public Set<Key> prev(Key key) {
        return reverseIndex.getOrDefault(key, Set.of());
    }

    public Set<Key> next(Key key) {
        return forwardIndex.getOrDefault(key, Set.of());
    }

    //

    public void bfs(Consumer<Key> consumer) {
        DAGUtil.bfs(allNodes, forwardIndex, reverseIndex, consumer);
    }

}
