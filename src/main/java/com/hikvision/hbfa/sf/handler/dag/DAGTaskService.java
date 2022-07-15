package com.hikvision.hbfa.sf.handler.dag;

import com.hikvision.hbfa.sf.config.SubmitManager;
import com.hikvision.hbfa.sf.config.TaskProperties;
import com.hikvision.hbfa.sf.dag.DAGNodeHandler;
import com.hikvision.hbfa.sf.dag.DAGResult;
import com.hikvision.hbfa.sf.dag.DAGTask;
import com.hikvision.hbfa.sf.dto.SubtaskDto;
import com.hikvision.hbfa.sf.dto.TaskDto;
import com.hikvision.hbfa.sf.dto.TaskStartDto;
import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.ex.NotFoundException;
import com.hikvision.hbfa.sf.handler.call.CallableManager;
import com.hikvision.hbfa.sf.handler.dag.data.*;
import com.hikvision.hbfa.sf.handler.dag.impl.DAGNodeService;
import com.hikvision.hbfa.sf.handler.dag.impl.JsonDAGParamMaker;
import com.hikvision.hbfa.sf.util.ExpiringCache;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.MapUtil;
import com.hikvision.hbfa.sf.util.SimpleCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.hikvision.hbfa.sf.util.ParameterUtil.*;

@Slf4j
@Service
public class DAGTaskService {

    @Autowired
    SequenceGenerator<String> IDGenerator;

    @Autowired
    SubmitManager submitManager;

    @Autowired
    ExecutorService taskExecutor;

    @Autowired
    SimpleCache<Object, DAGNode> nodeCache;
    @Autowired
    SimpleCache<Object, DAGWorkflow> workflowCache;
    @Autowired
    ExpiringCache<String, DAGCachedTask> taskCache;
    @Autowired
    ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache;

    @Autowired
    CallableManager callableManager;
    @Autowired
    DAGNodeService dagNodeService;

    @Autowired
    TaskProperties properties;

    private DAGResult<Map<String, Object>> makeParams(
            String taskId, Map<String, Object> input,
            Map<String, DAGResult<Map<String, Object>>> results,
            DAGWorkflow workflow) {
        var arguments = new ObjectMap(4);
        arguments.put(KEY_TASK, Map.of(
                KEY_ID, taskId,
                KEY_INPUT, MapUtil.nonnull(input)
        ));

        boolean setError = false;
        var errors = new ArrayList<String>();
        for (var e : results.entrySet()) {
            var name = e.getKey();
            var result = e.getValue();
            if (!result.isSuccess()) {
                setError = true;
                errors.add(result.getError());
                log.debug("Task-{}: Node-{} error: {}", taskId, name, result.getError());
            } else {
                log.debug("Task-{}: Node-{} success", taskId, name);
                arguments.put(name,
                        argumentMap(result.getData(), result.getError()));
            }
        }
        var output = replaceAllParams(workflow.outputParameters(), arguments);
        return new DAGResult<>(!setError, output, String.join(",", errors));
    }


    /**
     * @param workflowKey workflow的id或name
     * @param input       输入arguments集
     * @param timeout     超时（毫秒）
     * @return 输出结果的Future
     */
    public CompletableFuture<DAGTaskResult>
    runTask(Object workflowKey, Map<String, Object> input, long timeout) {
        if (StringUtils.isEmpty(workflowKey)) {
            throw new IllegalArgumentException("require workflow");
        }
        var workflow = workflowCache.get(workflowKey);
        if (null == workflow) {
            log.debug("Workflow-{} not found", workflowKey);
            throw new NotFoundException(workflowKey.toString());
        }
        if (timeout <= 0) {
            timeout = workflow.timeout();
        }
        String taskId = IDGenerator.next();
        log.debug("Run Task-{} of Workflow-{} timeout in {}ms with input: {}",
                taskId, workflowKey, timeout, input);
        var startTime = Instant.now();

        // 用于收集子任务ID
        var subtaskNodeMap = new ConcurrentHashMap<String, String>();
        // 用于格式化节点的输入参数
        var paramMaker = new JsonDAGParamMaker(taskId,
                MapUtil.nonnull(input), nodeCache, workflow, submitManager);
        // 节点执行
        var handler = new DAGNodeHandler<>(IDGenerator,
                paramMaker, dagNodeService.nodeExecutor(taskId, subtaskNodeMap));
        // 创建和启动任务
        var task = new DAGTask<>(workflow.graph(), handler);
        taskExecutor.execute(task);

        // 缓存起来
        var cachedTask = new DAGCachedTask(taskId, workflow.name(), task,
                Collections.unmodifiableMap(subtaskNodeMap));
        taskCache.set(taskId, cachedTask, timeout).thenAcceptAsync(e -> {
            log.warn("Task-{} timeout, will cancel now", taskId);
            task.cancel(false);
            e.getValue().task().completeExceptionally(
                    new CallTimeoutException("Task-" + taskId + " timeout"));
        }, taskExecutor);

        return task.thenCompose(results -> {
            if (log.isTraceEnabled()) {
                log.trace("Task-{} complete: {}", taskId, JsonUtil.lazyJson(results));
            } else {
                log.debug("Task-{} complete", taskId);
            }
            var result = makeParams(taskId, input, results, workflow);
            log.debug("Task-{} complete with result: {}", taskId, JsonUtil.lazyJson(result));

            var taskResult = new DAGTaskResult(result.isSuccess(), taskId, workflow,
                    input, result.getData(), task, subtaskNodeMap, startTime, result.getError());

            if (null == workflow.notifier()) {
                return CompletableFuture.completedFuture(taskResult);
            }

            log.debug("Task-{} complete and notify {}", taskId, workflow.notifier());
            var callable = callableManager.get(workflow.notifier());
            return callable.asyncCall(taskId, workflow.notifierConfig(), result.getData())
                    .thenApply(r -> taskResult);
        }).whenComplete((r, e) -> {
            taskCache.del(taskId);
            if (null == e) {
                log.debug("Task-{} complete.", taskId);
            } else {
                log.error("Task-{} error", taskId, e);
            }
        });
    }

    public CompletableFuture<TaskDto> callTask(TaskStartDto start) {
        var verbose = start.isVerbose();
        var future = runTask(start.getName(), start.getInput(), start.getTimeout());
        return future.thenApply(result -> {
            var dto = new TaskDto();
            dto.setId(result.id());
            dto.setWorkflowId(result.workflow().id());
            dto.setWorkflowName(result.workflow().name());
            dto.setInput(result.input());
            dto.setSuccess(result.success());
            dto.setOutput(result.output());
            dto.setCreatedAt(result.startTime());
            dto.setUpdatedAt(Instant.now());
            dto.setErrMsg(result.error());
            // TODO 更多数据
            var originalResults = result.task().results();
            var subtasks = result.subtaskNodeMap();
            var subs = new ArrayList<SubtaskDto>(subtasks.size());
            dto.setSubtasks(subs);
            for (var e : subtasks.entrySet()) {
                var sd = new SubtaskDto();
                subs.add(sd);
                sd.setId(e.getKey());
                sd.setNodeName(e.getValue());
                var r = originalResults.get(e.getValue());
                if (null == r) {
                    log.warn("Subtask-{}/Node-{} has no result", e.getKey(), e.getValue());
                } else {
                    sd.setSuccess(r.isSuccess());
                    if (verbose) {
                        sd.setOutput(r.getData());
                    }
                    sd.setErrMsg(r.getError());
                }
            }
            return dto;
        });
    }

    public void submitAsync(String subtaskId, Map<String, Object> output) {
        log.debug("Subtask-{} submit async", subtaskId);
        var asyncSubtask = asyncSubtaskCache.del(subtaskId);
        if (null == asyncSubtask) {
            log.error("subtask-{} not exists or has expired", subtaskId);
            return;
        }
        log.debug("Subtask-{}/Node-{} submit async",
                subtaskId, asyncSubtask.subtask().node().name());

        try {
            var node = asyncSubtask.subtask().node();
            var codeVal = readParam(output, node.submitCode());
            var code = node.codeOf(null != codeVal ? codeVal.toString() : null);
            var msg = readParam(output, node.submitMsg(), String.class);
            asyncSubtask.complete(new DAGResult<>(code == ResultCode.SUCCESS, output, msg));
            log.debug("Subtask-{}/Node-{} submit {}: {}", subtaskId, node.name(), code, msg);
        } catch (Throwable e) {
            log.error("Subtask-{} complete error", subtaskId, e);
            asyncSubtask.completeExceptionally(e);
        }
    }


    public TaskDto getTask(String taskId) {
        var ct = taskCache.get(taskId);
        if (null == ct) {
            throw new NotFoundException();
        }
        var results = ct.task().results();
        var dto = new TaskDto();
        var subtasks = ct.subtaskNodeMap();
        var subs = new ArrayList<SubtaskDto>(subtasks.size());
        dto.setSubtasks(subs);
        for (var e : subtasks.entrySet()) {
            String id = e.getKey();
            var key = e.getValue();
            var node = nodeCache.get(key);

            var sd = new SubtaskDto();
            subs.add(sd);
            sd.setId(id);
            sd.setNodeId(node.id());
            sd.setNodeName(node.name());
            var r = results.get(key);
            if (null != r) {
                sd.setSuccess(r.isSuccess());
                sd.setOutput(r.getData());
                sd.setErrMsg(r.getError());
            }
        }
        return dto;
    }


    public Map<String, Object> stats(boolean verbose) {
        var data = new LinkedHashMap<String, Object>(4);
        data.put("node", Map.of("size", nodeCache.size()));
        data.put("workflow", Map.of("size", workflowCache.size()));
        if (verbose) {
            var ids = taskCache.stream((s, t) -> t.id()).collect(Collectors.toList());
            data.put("task", Map.of(
                    "size", taskCache.size(),
                    "maxSize", taskCache.maxSize(),
                    "list", ids
            ));
        } else {

            data.put("task", Map.of(
                    "size", taskCache.size(),
                    "maxSize", taskCache.maxSize()
            ));
        }
        data.put("subtask", Map.of(
                "size", asyncSubtaskCache.size(),
                "sizeMax", asyncSubtaskCache.maxSize()
        ));
        return data;
    }

}
