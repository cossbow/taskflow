package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.json.Script;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.handler.script.ScriptExecutorsManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class ScriptDAGNodeExecutor
        extends ConfigDAGNodeExecutor<Script> {

    final ScriptExecutorsManager scriptExecutorsManager;
    final Executor executor;

    public ScriptDAGNodeExecutor(ScriptExecutorsManager scriptExecutorsManager,
                                 Executor executor) {
        super(Script.class);
        this.scriptExecutorsManager = scriptExecutorsManager;
        this.executor = executor;
    }

    @Override
    public NodeType type() {
        return NodeType.SCRIPT;
    }

    @Override
    public CompletableFuture<DAGResult<Map<String, Object>>>
    doExec(DAGSubtask subtask, Script script, DAGNode node, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> {
            var function = scriptExecutorsManager.getFunction(node.id(), script);
            var output = function.apply(input);
            return DAGResult.success(output);
        }, executor);
    }

}
