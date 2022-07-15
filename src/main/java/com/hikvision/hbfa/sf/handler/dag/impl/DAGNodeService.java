package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.dag.TriFunction;
import com.hikvision.hbfa.sf.handler.dag.RetryException;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.util.RetryFuture;
import com.hikvision.hbfa.sf.util.SimpleCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

@Component
public class DAGNodeService {

    @Autowired
    SimpleCache<Object, DAGNode> nodeCache;
    @Autowired
    DAGNodeHandlerManager dagNodeHandlerManager;

    public TriFunction<String, String, Map<String, Object>,
            CompletableFuture<DAGResult<Map<String, Object>>>>
    nodeExecutor(String taskId, ConcurrentMap<String, String> subtaskNodeMap) {
        return (subtaskId, nodeKey, input) -> {
            var node = nodeCache.get(nodeKey);
            var executor = dagNodeHandlerManager.apply(node.type());
            var subtask = new DAGSubtask(subtaskId, taskId, node, input);

            try {
                if (node.maxRetries() > 0) {
                    return RetryFuture.builder(() -> executor.exec(subtask))
                            .on(RetryException.class).retries(node.maxRetries()).build();
                } else {
                    try {
                        return executor.exec(subtask);
                    } catch (Throwable e) {
                        return CompletableFuture.failedFuture(e);
                    }
                }
            } finally {
                if (null != subtaskNodeMap) {
                    subtaskNodeMap.put(subtaskId, node.name());
                }
            }
        };
    }

}
