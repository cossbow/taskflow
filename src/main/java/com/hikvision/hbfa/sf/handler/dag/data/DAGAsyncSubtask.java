package com.hikvision.hbfa.sf.handler.dag.data;

import com.hikvision.hbfa.sf.dag.DAGResult;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DAGAsyncSubtask extends
        CompletableFuture<DAGResult<Map<String, Object>>> {
    private final DAGSubtask subtask;

    public DAGAsyncSubtask(DAGSubtask subtask) {
        this.subtask = Objects.requireNonNull(subtask);
    }

    public DAGSubtask subtask() {
        return subtask;
    }
}
