package com.hikvision.hbfa.sf.controller;

import com.hikvision.hbfa.sf.dto.TaskStartDto;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.handler.dag.DAGTaskService;
import com.hikvision.hbfa.sf.service.TaskSourceService;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@Slf4j
@Validated
@RequestMapping(ParameterUtil.TASK_PATH)
@RestController
public class TaskController {

    @Autowired
    DAGTaskService dagTaskService;
    @Autowired
    TaskSourceService taskSourceService;

    @Autowired
    ExecutorService taskExecutor;


    @PostMapping("start")
    public CompletableFuture<BaseResult<?>> start(@RequestBody @Valid TaskStartDto dto) {
        return call(dto);
    }

    @PostMapping("call")
    public CompletableFuture<BaseResult<?>> call(@RequestBody @Valid TaskStartDto dto) {
        log.debug("run Task of {} by API", dto.getName());
        var future = dagTaskService.callTask(dto);
        return future.thenApply(successFun());
    }


    @RequestMapping(ParameterUtil.SUBMIT_PATH + "{id}")
    public CompletableFuture<?> submit(@PathVariable String id,
                                       @RequestBody @Valid @NotNull ObjectMap dto) {
        return CompletableFuture.runAsync(() -> {
            log.debug("async submit Subtask-{} through api: {}", id, dto);
            dagTaskService.submitAsync(id, dto);
        }, taskExecutor);
    }

    @GetMapping("/detail/{key}")
    public BaseResult<?> getTask(@PathVariable String key) {
        var task = dagTaskService.getTask(key);
        return BaseResult.success(task);
    }


    @PostMapping("/taskSource/{name}")
    public BaseResult<?> startTaskSource(@PathVariable String name) {
        taskSourceService.start(name);
        return BaseResult.success();
    }

    @DeleteMapping("/taskSource/{name}")
    public CompletableFuture<BaseResult<?>> stopTaskSource(@PathVariable String name) {
        return taskSourceService.stop(name).thenApply(successFun());
    }

    private static <R> Function<R, BaseResult<R>> successFun() {
        return BaseResult::success;
    }

}
