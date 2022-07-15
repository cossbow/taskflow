package com.hikvision.hbfa.sf.handler.dag.data;

import java.util.Map;
import java.util.Objects;


public class DAGSubtask {
    private final String id;
    private final String taskId;
    private final DAGNode node;
    private final Map<String, Object> input;

    public DAGSubtask(String id, String taskId,
                      DAGNode node, Map<String, Object> input) {
        this.id = Objects.requireNonNull(id);
        this.taskId = Objects.requireNonNull(taskId);
        this.node = Objects.requireNonNull(node);
        this.input = Objects.requireNonNull(input);
    }

    public String id() {
        return id;
    }

    public String taskId() {
        return taskId;
    }

    public DAGNode node() {
        return node;
    }

    public Map<String, Object> input() {
        return input;
    }


    @Override
    public String toString() {
        return "Subtask(" + node.name() + "-" + id + ')';
    }

}
