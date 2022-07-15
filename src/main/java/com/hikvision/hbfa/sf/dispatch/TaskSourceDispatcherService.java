package com.hikvision.hbfa.sf.dispatch;

import com.hikvision.hbfa.sf.entity.TaskSource;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceStatus;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceType;
import com.hikvision.hbfa.sf.handler.dag.DAGTaskService;
import com.hikvision.hbfa.sf.util.TypedBeanManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TaskSourceDispatcherService extends TypedBeanManager<TaskSourceType, TaskSourceDispatcherFactory> {

    private final Map<String, TaskSourceDispatcher> dispatchers = new ConcurrentHashMap<>();

    private final DAGTaskService dagTaskService;

    public TaskSourceDispatcherService(List<TaskSourceDispatcherFactory> factories,
                                       DAGTaskService dagTaskService) {
        super("TaskSourceDispatcherFactories", factories);
        this.dagTaskService = dagTaskService;
    }


    public void start(TaskSource source) {
        dispatchers.computeIfAbsent(source.getName(), name -> {
            log.info("start TaskSource-{}", name);
            var factory = this.get(source.getType());
            log.info("do create TaskSource-{}", name);
            var dispatcher = factory.create(source, (param, input) -> {
                log.debug("run task by TaskSource-{}", param.getName());
                return dagTaskService.runTask(param.getWorkflow(), input, param.getTimeout());
            });
            log.info("do start TaskSource-{}", name);
            dispatcher.start();
            log.info("TaskSource-{} running", name);
            return dispatcher;
        });
    }

    public CompletableFuture<Void> stop(String sourceName) {
        log.info("stopping TaskSource-{} ...", sourceName);
        var dispatcher = dispatchers.get(sourceName);
        if (null == dispatcher) {
            return CompletableFuture.completedFuture(null);
        }
        return dispatcher.stop().thenAccept(v -> {
            log.info("success stopped TaskSource-{}", sourceName);
            dispatchers.remove(sourceName);
        });
    }

    public TaskSourceStatus status(String sourceName) {
        var dispatcher = dispatchers.get(sourceName);
        if (null == dispatcher) {
            return TaskSourceStatus.HALT;
        }

        return dispatcher.status();
    }

}
