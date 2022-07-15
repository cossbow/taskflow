package com.hikvision.hbfa.sf.handler.dag.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.handler.dag.DAGNodeExecutor;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.util.JsonUtil;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class ConfigDAGNodeExecutor<C>
        implements DAGNodeExecutor {

    private final LoadingCache<String, C> configCache;

    protected ConfigDAGNodeExecutor(Class<C> configType) {
        configCache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .softValues()
                .build(JsonUtil.cacheLoader(configType));
    }


    @Override
    public CompletableFuture<DAGResult<Map<String, Object>>>
    exec(DAGSubtask subtask) {
        C config = configCache.get(subtask.node().config());
        assert null != config;
        return doExec(subtask, config, subtask.node(), subtask.input());
    }

    protected abstract CompletableFuture<DAGResult<Map<String, Object>>>
    doExec(DAGSubtask subtask, C config, DAGNode node, Map<String, Object> input);

}
