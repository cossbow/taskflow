package com.hikvision.hbfa.sf.dag;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;


public class DAGNodeHandler<ID, K, D>
        implements BiFunction<K, Map<K, DAGResult<D>>,
        CompletableFuture<DAGResult<D>>> {

    final Supplier<ID> IDGenerator;
    final TriFunction<ID, K, Map<K, DAGResult<D>>, DAGResult<D>> paramMaker;
    final TriFunction<ID, K, D, CompletableFuture<DAGResult<D>>> executor;

    public DAGNodeHandler(Supplier<ID> IDGenerator,
                          TriFunction<ID, K, Map<K, DAGResult<D>>, DAGResult<D>> paramMaker,
                          TriFunction<ID, K, D, CompletableFuture<DAGResult<D>>> executor) {
        this.IDGenerator = IDGenerator;
        this.paramMaker = paramMaker;
        this.executor = executor;
    }


    @Override
    public CompletableFuture<DAGResult<D>> apply(K nodeKey, Map<K, DAGResult<D>> dependentResults) {
        var subtaskId = IDGenerator.get();
        DAGResult<D> form;
        try {
            form = paramMaker.apply(subtaskId, nodeKey, dependentResults);
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
        if (!form.isSuccess()) {
            return CompletableFuture.completedFuture(form);
        }

        return executor.apply(subtaskId, nodeKey, form.getData());
    }

}
