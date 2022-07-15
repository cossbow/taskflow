package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.config.SubmitManager;
import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.handler.dag.SequenceGenerator;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.hikvision.hbfa.sf.util.ParameterUtil.*;

@Slf4j
@Component
public class BatchDAGNodeExecutor
        extends ConfigDAGNodeExecutor<
        BatchDAGNodeExecutor.BatchConfig> {

    final SequenceGenerator<String> IDGenerator;
    final SimpleCache<Object, DAGNode> nodeCache;
    final SubmitManager submitManager;
    final DAGNodeService dagNodeService;

    public BatchDAGNodeExecutor(@Lazy DAGNodeService dagNodeService,
                                SimpleCache<Object, DAGNode> nodeCache,
                                SubmitManager submitManager,
                                SequenceGenerator<String> IDGenerator) {
        super(BatchConfig.class);
        this.dagNodeService = dagNodeService;
        this.nodeCache = nodeCache;
        this.submitManager = submitManager;
        this.IDGenerator = IDGenerator;
    }


    @Override
    public NodeType type() {
        return NodeType.BATCH;
    }

    @Override
    public CompletableFuture<DAGResult<Map<String, Object>>>
    doExec(DAGSubtask subtask, BatchConfig config, DAGNode node, Map<String, Object> input) {
        var id = subtask.id();
        log.debug("Subtask-{} call BATCH", id);
        if (ValueUtil.isEmpty(config.getNodeName())) {
            return CompletableFuture.completedFuture(DAGResult.error(
                    "config.nodeName is required."));
        }
        if (ValueUtil.isEmpty(config.getDataKey())) {
            return CompletableFuture.completedFuture(DAGResult.error(
                    "config.dataKey is required."));
        }
        log.debug("Subtask-{} batch call Node-{}", id, node.name());
        var dataKey = config.getDataKey();

        var l = input.get(dataKey);
        if (!(l instanceof Collection)) {
            return CompletableFuture.completedFuture(DAGResult.error(
                    "value of '" + dataKey + "' is not Collection"));
        }
        var c = (Collection<?>) l;

        var subNode = nodeCache.get(config.getNodeName());
        Map<String, Object> inputParameters;
        var defaultArguments = subNode.defaultArguments();
        var forkParameters = config.getForkParameters();
        if (null == defaultArguments) {
            inputParameters = forkParameters;
        } else {
            if (null == forkParameters) {
                inputParameters = defaultArguments;
            } else {
                var temp = new HashMap<>(defaultArguments);
                MapUtil.deepMerge(temp, forkParameters);
                inputParameters = temp;
            }
        }

        var futures = new ArrayList<CompletableFuture<DAGResult<Map<String, Object>>>>(c.size());
        var executor = dagNodeService.nodeExecutor(subtask.taskId(), null);
        for (var arg : c) {
            var subSubId = IDGenerator.next();
            log.debug("Subtask-{}'s batch call Subtask-{} argument: {}", id, subSubId, JsonUtil.lazyJson(arg));
            var arguments = new HashMap<String, Object>(5);
            arguments.put(KEY_ID, subSubId);
            arguments.put(KEY_TASK, Map.of(KEY_ID, subtask.taskId()));
            arguments.put(KEY_INPUT, arg);
            arguments.put(KEY_NAME, subNode.name());
            if (subNode.async()) {
                submitManager.addSubmitArguments(arguments, subNode.id(), subSubId);
            }
            var subInput = replaceAllParams(inputParameters, arguments);
            log.debug("Subtask-{}'s batch call Subtask-{}: {}", id, subSubId, JsonUtil.lazyJson(subInput));
            var future = executor.apply(subSubId, config.getNodeName(), subInput);
            future.whenComplete((r, e) -> {
                if (null == e) {
                    if (r.isSuccess()) {
                        log.debug("Subtask-{}/Batch-{} success: {}", id, subSubId, JsonUtil.lazyJson(r.getData()));
                    } else {
                        log.debug("Subtask-{}/Batch-{} error: {}", id, subSubId, r.getError());
                    }
                } else {
                    log.debug("Subtask-{}/Batch-{} exception", id, subSubId, e);
                }
            });
            futures.add(future);
        }

        var faultTolerant = config.faultTolerant;
        var joinParameters = config.getJoinParameters();
        return FutureUtil.collect(futures, Collectors.toList()).thenApply(results -> {
            var list = new ArrayList<Map<String, Object>>(results.size());
            var errors = new ArrayList<String>(results.size());
            for (var result : results) {
                if (result.isSuccess()) {
                    list.add(result.getData());
                } else {
                    errors.add(result.getError());
                }
            }

            // 全错
            if (list.isEmpty()) {
                return DAGResult.error(String.join(",", errors));
            }
            // 部分错
            if (!errors.isEmpty()) {
                // 容错
                if (faultTolerant) {
                    return new DAGResult<>(true,
                            replaceAllParams(joinParameters, Map.of(dataKey, list)),
                            String.join(",", errors));
                } else {
                    return DAGResult.error(String.join(",", errors));
                }
            }
            // 无错
            return DAGResult.success(replaceAllParams(joinParameters, Map.of(dataKey, list)));
        });
    }


    @Data
    static class BatchConfig {
        private String nodeName;
        private String dataKey = "data";
        private Map<String, Object> forkParameters;
        private Map<String, Object> joinParameters;
        private boolean faultTolerant;
    }

}
