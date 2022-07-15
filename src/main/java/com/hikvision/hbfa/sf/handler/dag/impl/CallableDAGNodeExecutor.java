package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.handler.call.Callable;
import com.hikvision.hbfa.sf.handler.dag.CallTimeoutException;
import com.hikvision.hbfa.sf.handler.dag.DAGNodeExecutor;
import com.hikvision.hbfa.sf.handler.dag.RetryException;
import com.hikvision.hbfa.sf.handler.dag.data.DAGAsyncSubtask;
import com.hikvision.hbfa.sf.handler.dag.data.DAGSubtask;
import com.hikvision.hbfa.sf.util.ExpiringCache;
import com.hikvision.hbfa.sf.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
public class CallableDAGNodeExecutor implements DAGNodeExecutor {

    private final NodeType type;
    private final Callable<?> callable;
    private final ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache;
    private final Executor executor;

    public CallableDAGNodeExecutor(
            NodeType type,
            Callable<?> callable,
            ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache,
            Executor executor) {
        this.type = type;
        this.callable = callable;
        this.asyncSubtaskCache = asyncSubtaskCache;
        this.executor = executor;
    }

    @Override
    public NodeType type() {
        return type;
    }

    @Override
    public CompletableFuture<DAGResult<Map<String, Object>>>
    exec(DAGSubtask subtask) {
        var call = callable.asyncCall(subtask.id(), subtask.node().config(), subtask.input());
        // 异步调用返回成功，缓存并等待回传数据
        var timeout = subtask.node().timeout();
        var asyncSubtask = new DAGAsyncSubtask(subtask);
        log.debug("Subtask-{} put in cache, expire in {}",
                subtask.id(), timeout);
        asyncSubtaskCache.set(subtask.id(), asyncSubtask, timeout).thenAcceptAsync(e -> {
            var s = e.getValue();
            log.warn("Subtask-{}/Node-{} expired: {}",
                    s.subtask().id(), s.subtask().node().name(),
                    JsonUtil.lazyJson(s.subtask().input()));
            call.cancel(false);
            s.completeExceptionally(new CallTimeoutException(
                    "Subtask-" + s.subtask().id() + " timeout."));
        }, executor);
        call.whenCompleteAsync((ret, e) -> {
            var node = subtask.node();
            if (null != e) {
                log.error("Subtask-{} sync-call Node-{} error", subtask.id(), node.name(), e);
                asyncSubtaskCache.del(subtask.id());
                asyncSubtask.completeExceptionally(e);
                return;
            }

            // 同步任务，直接处理结果
            if (!node.async()) {
                if (log.isTraceEnabled()) {
                    log.trace("Subtask-{} sync-call Node-{} get {}: {}; {}", subtask.id(),
                            node.name(), ret.getCode(), ret.getMsg(), JsonUtil.lazyJson(ret.getData()));
                } else {
                    log.debug("Subtask-{} sync-call Node-{} get {}: {}", subtask.id(),
                            node.name(), ret.getCode(), ret.getMsg());
                }
                var result = new DAGResult<>(ret.getCode() == ResultCode.SUCCESS,
                        ret.getData(), ret.getMsg());
                asyncSubtaskCache.del(subtask.id());
                asyncSubtask.complete(result);
                return;
            }

            // 异步任务
            if (ret.getCode() == ResultCode.NETWORK) {
                log.warn("Subtask-{} async-call Node-{} get {}: {}; {}", subtask.id(),
                        node.name(), ret.getCode(), ret.getMsg(), JsonUtil.lazyJson(ret.getData()));
                asyncSubtaskCache.del(subtask.id());
                asyncSubtask.completeExceptionally(new RetryException(ret.getMsg()));
            } else if (ret.getCode() == ResultCode.TASK_ERROR) {
                log.error("Subtask-{} async-call Node-{} get {}: {}; {}", subtask.id(),
                        node.name(), ret.getCode(), ret.getMsg(), JsonUtil.lazyJson(ret.getData()));
                asyncSubtaskCache.del(subtask.id());
                asyncSubtask.complete(DAGResult.error(ret.getMsg()));
            } else if (ret.getCode() != ResultCode.SUCCESS) {
                asyncSubtaskCache.del(subtask.id());
                asyncSubtask.completeExceptionally(new IllegalStateException(ret.getMsg()));
            } else {
                log.debug("Subtask-{} async-call Node-{} get {}: {}; {}", subtask.id(),
                        node.name(), ret.getCode(), ret.getMsg(), JsonUtil.lazyJson(ret.getData()));
                // 等待异步任务响应
            }

        }, executor);
        return asyncSubtask;
    }


}
