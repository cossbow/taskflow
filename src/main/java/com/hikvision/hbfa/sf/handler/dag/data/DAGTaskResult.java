package com.hikvision.hbfa.sf.handler.dag.data;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.dag.DAGTask;

import java.time.Instant;
import java.util.Map;

public class DAGTaskResult {
    private final boolean success;
    private final String id;
    private final DAGWorkflow workflow;
    private final Map<String, Object> input;
    private final Map<String, Object> output;
    private final DAGTask<String, DAGResult<Map<String, Object>>> task;
    private final Map<String, String> subtaskNodeMap;
    private final Instant startTime;
    private final String error;

    public DAGTaskResult(boolean success,
                         String id,
                         DAGWorkflow workflow,
                         Map<String, Object> input, Map<String, Object> output,
                         DAGTask<String, DAGResult<Map<String, Object>>> task,
                         Map<String, String> subtaskNodeMap,
                         Instant startTime,
                         String error) {
        this.success = success;
        this.id = id;
        this.workflow = workflow;
        this.input = input;
        this.output = output;
        this.task = task;
        this.subtaskNodeMap = subtaskNodeMap;
        this.startTime = startTime;
        this.error = error;
    }

    public boolean success() {
        return success;
    }

    public String id() {
        return id;
    }

    public DAGWorkflow workflow() {
        return workflow;
    }

    public Map<String, Object> input() {
        return input;
    }

    public Map<String, Object> output() {
        return output;
    }

    public DAGTask<String, DAGResult<Map<String, Object>>> task() {
        return task;
    }

    public Map<String, String> subtaskNodeMap() {
        return subtaskNodeMap;
    }

    public Instant startTime() {
        return startTime;
    }

    public String error() {
        return error;
    }
}
