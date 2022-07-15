package com.hikvision.hbfa.sf.dag;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.hikvision.hbfa.sf.dag.DAGTaskTest.TestNode.*;

public class DAGTaskTest {
    static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(16);

    @AfterAll
    public static void shutdown() {
        EXECUTOR.shutdown();
    }


    enum TestNode {
        N1,
        N2,
        N3,
        N4,
        N5,
        N6,
    }

    final DAGGraph<TestNode> graph;

    {
        var edges = List.of(
                Map.entry(N1, N2),
                Map.entry(N1, N3),
                Map.entry(N2, N4),
                Map.entry(N3, N4),
                Map.entry(N3, N5),
                Map.entry(N5, N6),
                Map.entry(N1, N6)
        );

        graph = new DAGGraph<>(List.of(values()), edges);
    }

    static int sumDAGResults(Collection<Integer> results) {
        return results.stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    public void testCalc() {
        final var executor = CompletableFuture.delayedExecutor(
                500, TimeUnit.MILLISECONDS, EXECUTOR);
        final var input = 1;
        var task = new DAGTask<TestNode, Integer>(graph, (k, results) -> {
            System.out.println(k + " input " + results);
            var in = results.isEmpty() ? input : sumDAGResults(results.values());
            return CompletableFuture.supplyAsync(() -> {
                System.out.println(k + " return " + (in + 1));
                return in + 1;
            }, executor);
        });
        EXECUTOR.execute(task);
        EXECUTOR.execute(task); // 重复执行无影响
        var re = task.join();
        var sum = sumDAGResults(re.values());
        System.out.println(sum);
    }

    @Test
    public void testCancelSubtask() {
        final var executor = CompletableFuture.delayedExecutor(
                1000, TimeUnit.MILLISECONDS, EXECUTOR);
        final var input = 1;
        var task = new DAGTask<TestNode, Integer>(graph, (k, results) -> {
            if (N3 == k) {
                var fc = new CompletableFuture<Integer>();
                executor.execute(() -> {
                    fc.cancel(false);
                });
                return fc;
            }
            var in = results.isEmpty() ? input : sumDAGResults(results.values());
            return CompletableFuture.supplyAsync(() -> {
                System.out.println(k + " return " + (in + 1));
                return in + 1;
            }, executor);
        });
        EXECUTOR.execute(task);

        try {
            task.join();
            Assert.fail("Task should be canceled.");
        } catch (CancellationException e) {
        }

    }

    @Test
    public void testCancelTask() {
        final var executor = CompletableFuture.delayedExecutor(
                500, TimeUnit.MILLISECONDS, EXECUTOR);
        final var input = 1;
        var task = new DAGTask<TestNode, Integer>(graph, (k, results) -> {
            var in = results.isEmpty() ? input : sumDAGResults(results.values());
            return CompletableFuture.supplyAsync(() -> {
                System.out.println(k + " return " + (in + 1));
                return in + 1;
            }, executor);
        });
        EXECUTOR.execute(task);
        CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS, EXECUTOR)
                .execute(() -> task.cancel(false));

        try {
            task.join();
            Assert.fail("Task should be canceled.");
        } catch (CancellationException e) {
        }

    }

}
