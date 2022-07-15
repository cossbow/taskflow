package com.hikvision.hbfa.sf.handler.dag.data;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.dag.DAGTask;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class DAGCachedTask {
    private final String id;
    private final String workflow;
    private final DAGTask<String, DAGResult<Map<String, Object>>> task;
    private final Map<String, String> subtaskNodeMap;

    public DAGCachedTask(String id, String workflow,
                         DAGTask<String, DAGResult<Map<String, Object>>> task,
                         Map<String, String> subtaskNodeMap) {
        this.id = requireNonNull(id);
        this.workflow = requireNonNull(workflow);
        this.task = requireNonNull(task);
        this.subtaskNodeMap = requireNonNull(subtaskNodeMap);
    }

    public String id() {
        return id;
    }

    public String workflow() {
        return workflow;
    }

    public DAGTask<String, DAGResult<Map<String, Object>>> task() {
        return task;
    }

    public Map<String, String> subtaskNodeMap() {
        return subtaskNodeMap;
    }

}
