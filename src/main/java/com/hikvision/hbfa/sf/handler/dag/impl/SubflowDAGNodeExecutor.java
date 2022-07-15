package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.handler.dag.DAGTaskService;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.handler.dag.data.DAGWorkflow;
import com.hikvision.hbfa.sf.handler.script.ScriptExecutorsManager;
import com.hikvision.hbfa.sf.util.SimpleCache;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class SubflowDAGNodeExecutor
        extends ConfigDAGNodeExecutor<
        SubflowDAGNodeExecutor.SubflowConfig> {

    public SubflowDAGNodeExecutor() {
        super(SubflowConfig.class);
    }

    @Lazy
    @Autowired
    private DAGTaskService dagTaskService;
    @Autowired
    SimpleCache<Object, DAGWorkflow> workflowCache;
    @Autowired
    ScriptExecutorsManager scriptExecutorsManager;

    @Override
    public NodeType type() {
        return NodeType.SUBFLOW;
    }

    @Override
    public CompletableFuture<DAGResult<Map<String, Object>>>
    doExec(DAGSubtask subtask, SubflowConfig config, DAGNode node, Map<String, Object> input) {
        log.debug("Subtask-{} call subflow-{}", subtask.id(), config.workflow);
        return dagTaskService.runTask(config.workflow, input, node.timeout()).thenApply(result -> {
            if (result.success())
                return DAGResult.success(result.output());
            return DAGResult.error(result.error());
        });
    }

    @Data
    static class SubflowConfig {
        private String workflow;
    }
}
