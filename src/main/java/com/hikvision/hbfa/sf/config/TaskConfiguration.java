package com.hikvision.hbfa.sf.config;

import com.hikvision.hbfa.sf.handler.dag.SequenceGenerator;
import com.hikvision.hbfa.sf.handler.dag.data.DAGAsyncSubtask;
import com.hikvision.hbfa.sf.handler.dag.data.DAGCachedTask;
import com.hikvision.hbfa.sf.handler.dag.data.DAGNode;
import com.hikvision.hbfa.sf.handler.dag.data.DAGWorkflow;
import com.hikvision.hbfa.sf.util.ExpiringCache;
import com.hikvision.hbfa.sf.util.SimpleCache;
import com.hikvision.hbfa.sf.util.TagThreadFactory;
import com.hikvision.hbfa.sf.util.ValueUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class TaskConfiguration {

    @Primary
    @Bean
    ExecutorService taskExecutor(
            TaskProperties properties) {
        var pool = properties.getThreadPool();
        return new ThreadPoolExecutor(pool.getThreads(), pool.getThreads(),
                0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(pool.getQueueSize()),
                new TagThreadFactory("Task"));
    }


    @Bean
    SequenceGenerator<String> IDGenerator() {
        return ValueUtil::newUUID;
    }

    @Bean
    SimpleCache<Object, DAGNode> nodeCache() {
        return new SimpleCache<>();
    }

    @Bean
    SimpleCache<Object, DAGWorkflow> workflowCache() {
        return new SimpleCache<>();
    }

    @Bean
    ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache(
            TaskProperties properties) {
        return new ExpiringCache<>(
                properties.getCacheExpires().toMillis());
    }

    @Bean
    ExpiringCache<String, DAGCachedTask> taskCache(
            TaskProperties properties) {
        return new ExpiringCache<>(
                properties.getCacheExpires().toMillis());
    }


}
