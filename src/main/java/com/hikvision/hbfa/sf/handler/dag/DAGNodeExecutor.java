package com.hikvision.hbfa.sf.handler.dag;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.util.TypedBean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DAGNodeExecutor extends TypedBean<NodeType> {

    CompletableFuture<DAGResult<Map<String, Object>>>
    exec(DAGSubtask subtask);

    //

    default CompletableFuture<DAGResult<Map<String, Object>>> success(Map<String, Object> data) {
        return CompletableFuture.completedFuture(DAGResult.success(data));
    }

    default CompletableFuture<DAGResult<Map<String, Object>>> error(String errMsg) {
        return CompletableFuture.completedFuture(DAGResult.error(errMsg));
    }

}
