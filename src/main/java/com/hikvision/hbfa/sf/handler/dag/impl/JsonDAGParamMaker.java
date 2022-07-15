package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.config.SubmitManager;
import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.dag.TriFunction;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGWorkflow;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.hikvision.hbfa.sf.util.ParameterUtil.*;

@Slf4j
public class JsonDAGParamMaker implements
        TriFunction<String, String, Map<String, DAGResult<Map<String, Object>>>,
                DAGResult<Map<String, Object>>> {

    private final String taskId;
    private final Map<String, Object> input;
    private final Function<Object, DAGNode> nodeSupplier;
    private final Function<String, DAGWorkflow.DAGNodeIo> ioSupplier;
    private final SubmitManager submitManager;

    public JsonDAGParamMaker(String taskId,
                             Map<String, Object> input, Function<Object, DAGNode> nodeSupplier,
                             Function<String, DAGWorkflow.DAGNodeIo> ioSupplier,
                             SubmitManager submitManager) {
        this.taskId = taskId;
        this.input = input;
        this.nodeSupplier = nodeSupplier;
        this.ioSupplier = ioSupplier;
        this.submitManager = submitManager;
    }

    @Override
    public DAGResult<Map<String, Object>> apply(
            String subtaskId, String currentName,
            Map<String, DAGResult<Map<String, Object>>> dependentResults) {
        // 检查失败条件
        for (var e : dependentResults.entrySet()) {
            var name = e.getKey();
            var result = e.getValue();
            var depIO = ioSupplier.apply(name);
            if (!result.isSuccess() && !depIO.ignoreError()) {
                return DAGResult.error("Node-" + name + " error and infect");
            }
        }

        var arguments = new HashMap<String, Object>(4);
        arguments.put(KEY_TASK, Map.of(
                KEY_ID, taskId,
                KEY_INPUT, input
        ));
        for (var e : dependentResults.entrySet()) {
            var name = e.getKey();
            var result = e.getValue();
            arguments.put(name, argumentMap(result.getData(), result.getError()));
        }
        arguments.put(KEY_ID, subtaskId);
        arguments.put(KEY_NAME, currentName);
        var node = nodeSupplier.apply(currentName);
        if (node.async()) {
            submitManager.addSubmitArguments(arguments, node.id(), subtaskId);
        }

        var io = ioSupplier.apply(currentName);
        try {
            return DAGResult.success(replaceAllParams(io.inputParameters(), arguments));
        } catch (Exception e) {
            log.error("Subtask-{}/Node-{} parameter parse error: ", subtaskId, node.name(), e);
            return DAGResult.error("parameter parse error: " + e.getMessage());
        }
    }

}
