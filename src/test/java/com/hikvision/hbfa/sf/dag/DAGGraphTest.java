package com.hikvision.hbfa.sf.dag;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DAGGraphTest {

    @Test
    public void testLegal() {
        var nodes = Set.of(1, 2, 3, 4);
        var edges = List.of(
                Map.entry(1, 2),
                Map.entry(1, 3),
                Map.entry(2, 4),
                Map.entry(3, 4)
        );
        new DAGGraph<>(nodes, edges);
    }

    @Test
    public void testIllegal1() {
        var nodes = Set.of(1, 2, 3, 4);
        var edges = List.of(
                Map.entry(1, 2),
                Map.entry(2, 3),
                Map.entry(3, 4),
                Map.entry(4, 1)
        );
        try {
            new DAGGraph<>(nodes, edges);
            Assert.fail("The graph is illegal");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testIllegal2() {
        var nodes = new HashSet<Integer>();
        for (int i = 1; i <= 6; i++) {
            nodes.add(i);
        }
        var edges = List.of(
                Map.entry(1, 2),
                Map.entry(1, 3),
                Map.entry(2, 4),
                Map.entry(3, 4),
                Map.entry(3, 5),
                Map.entry(5, 6),
                Map.entry(6, 1)
        );

        try {
            new DAGGraph<>(nodes, edges);
            Assert.fail("It's a cyclic graph");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    public void testKeys() {
        var nodes = Set.of(1, 2, 3, 4, 5, 6);
        var edges = List.of(
                Map.entry(1, 2),
                Map.entry(1, 3),
                Map.entry(2, 4),
                Map.entry(3, 4),
                Map.entry(1, 5),
                Map.entry(3, 5),
                Map.entry(6, 5)
        );
        var graph = new DAGGraph<>(nodes, edges);

        Assert.assertEquals(Set.of(1, 6), graph.heads());
        Assert.assertEquals(Set.of(4, 5), graph.tails());
    }

    @Test
    public void testDependency() {
        var nodes = List.of(1, 2, 3, 4);
        var edges = List.of(
                Map.entry(1, 2),
                Map.entry(1, 3),
                Map.entry(2, 4),
                Map.entry(3, 4)
        );
        var r = new DAGGraph<>(nodes, edges);
        Assert.assertEquals(Set.copyOf(nodes), r.allNodes());

        Assert.assertEquals(Set.of(1), r.heads());
        Assert.assertEquals(Set.of(2, 3), r.next(1));
        Assert.assertEquals(Set.of(4), r.next(2));
        Assert.assertEquals(Set.of(4), r.next(3));
        Assert.assertTrue(r.next(4).isEmpty());

        Assert.assertEquals(Set.of(4), r.tails());
        Assert.assertEquals(Set.of(2, 3), r.prev(4));
        Assert.assertEquals(Set.of(1), r.prev(2));
        Assert.assertEquals(Set.of(1), r.prev(3));
        Assert.assertTrue(r.prev(1).isEmpty());
    }


    @Test
    public void testBFS() {
        var nodes = Set.of(1, 2, 3, 4, 5, 6);
        var edges = List.of(
                Map.entry(1, 2),
                Map.entry(1, 3),
                Map.entry(2, 4),
                Map.entry(2, 5),
                Map.entry(3, 4),
                Map.entry(5, 6)
        );
        DAGUtil.bfs(nodes, edges, i -> {
            System.out.println(i);
        });
    }

    @Test
    public void testRand() {
        var size = ThreadLocalRandom.current().nextInt(10, 1000);
        var r = randDAG(size);
        r.bfs(i -> {
            System.out.println(i);
        });
    }

    //

    static DAGGraph<Integer> randDAG(int size) {
        return randDAG(size, Function.identity());
    }

    static <Key> DAGGraph<Key> randDAG(int size, Function<Integer, Key> mapper) {
        if (size <= 0) throw new IllegalArgumentException();

        var rand = ThreadLocalRandom.current();
        var nodes = rand
                .ints(1, size * 10)
                .boxed().distinct().limit(size).sorted().map(mapper).collect(Collectors.toList());
        var edges = new ArrayList<Map.Entry<Key, Key>>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                if (rand.nextInt() % 2 == 0) {
                    edges.add(Map.entry(nodes.get(i), nodes.get(j)));
                }
            }
        }

        return new DAGGraph<Key>(nodes, edges);
    }

}
