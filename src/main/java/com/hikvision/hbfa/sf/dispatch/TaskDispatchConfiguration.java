package com.hikvision.hbfa.sf.dispatch;

import com.hikvision.hbfa.sf.service.impl.NodeServiceImpl;
import com.hikvision.hbfa.sf.service.impl.TaskSourceServiceImpl;
import com.hikvision.hbfa.sf.service.impl.WorkflowServiceImpl;
import com.hikvision.hbfa.sf.util.FutureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 不在事务中，调用其他在事务中的方法
 */
@Slf4j
@ConfigurationProperties(prefix = "task.dispatcher")
@Component
public class TaskDispatchConfiguration implements ApplicationRunner {

    @Autowired
    private NodeServiceImpl nodeService;
    @Autowired
    private WorkflowServiceImpl workflowService;
    @Autowired
    private TaskSourceServiceImpl taskSourceService;

    @Autowired
    private ExecutorService taskExecutor;


    @Override
    public void run(ApplicationArguments args) {
        // 启动所有Node的订阅
        FutureUtil.runConcurrent(List.of(
                () -> nodeService.initNodes(),
                () -> workflowService.initWorkflows()),
                taskExecutor).join();
        taskSourceService.initTaskSources();
    }


}
