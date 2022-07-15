package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.json.Script;
import com.hikvision.hbfa.sf.handler.dag.DAGTaskService;
import com.hikvision.hbfa.sf.handler.dag.RetryException;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.handler.script.ScriptExecutorsManager;
import com.hikvision.hbfa.sf.util.ValueUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Component
public class DecisionDAGNodeExecutor
        extends ConfigDAGNodeExecutor<
        DecisionDAGNodeExecutor.DecisionConfig> {

    public DecisionDAGNodeExecutor() {
        super(DecisionConfig.class);
    }

    @Autowired
    ScriptExecutorsManager scriptExecutorsManager;
    @Lazy
    @Autowired
    private DAGTaskService dagTaskService;

    public NodeType type() {
        return NodeType.DECISION;
    }

    private CompletableFuture<DAGResult<Map<String, Object>>>
    runBranch(String nodeName, String subName, Map<String, Object> input, long timeout) {
        return dagTaskService.runTask(subName, input, timeout).thenApply(sr -> {
            log.debug("Node-{}/Branch-Flow-{} execute success", nodeName, subName);
            return new DAGResult<>(sr.success(), sr.output(), sr.error());
        }).exceptionally(e -> {
            if (e instanceof CompletionException) e = e.getCause();
            if (e instanceof IOException) {
                throw new RetryException(e);
            }
            log.error("Node-{}/Branch-Flow-{} execute error", nodeName, subName, e);
            return DAGResult.error(e.getMessage());
        });
    }

    @Override
    public CompletableFuture<DAGResult<Map<String, Object>>>
    doExec(DAGSubtask subtask, DecisionConfig config, DAGNode node, Map<String, Object> input) {
        log.debug("Subtask-{} call DECISION", subtask.id());
        if (ValueUtil.isEmpty(config.branches)) {
            return CompletableFuture.completedFuture(DAGResult.error("branches empty"));
        }

        var timeout = node.timeout();
        var nodeName = node.name();

        for (var branch : config.branches) {
            var subName = branch.workflow;
            var condition = scriptExecutorsManager.getCacheCondition(branch.rule);
            if (!condition.test(input)) {
                log.debug("Node-{} check Condition Branch-Node-{}: Not satisfied",
                        nodeName, subName);
                continue;
            }

            log.debug("Node-{} run Branch-Flow-{}", nodeName, subName);
            return runBranch(nodeName, subName, input, timeout);
        }

        return CompletableFuture.completedFuture(DAGResult.success(Map.of()));
    }


    @Data
    static class Branch {
        private String workflow;
        private Script rule;
    }

    @Data
    static class DecisionConfig {
        private List<Branch> branches;
    }

}
