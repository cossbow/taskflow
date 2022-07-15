package com.hikvision.hbfa.sf.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "task")
@Getter
@Setter
public class TaskProperties {

    // autoDeleteTask=false时有效，周期删除expireDays天以前的任务
    private int expireDays = 7;     // 保存一个星期

    private boolean enableTaskLog = false;

    // 消息重试次数
    private int queueRetries = 5;


    private final TaskThreadPool threadPool = new TaskThreadPool();


    private Duration cacheExpires = Duration.ofMinutes(1);

    private final Dispatcher dispatcher = new Dispatcher();


    @Getter
    @Setter
    public static class TaskThreadPool {
        // 任务线程池大小
        private int threads = 100;
        // 任务线程池队列长度
        private int queueSize = 1000;
    }

    @Getter
    @Setter
    public static class Dispatcher {

        private int messageConcurrency = 20;

        private int messagePullInterval = 1000;

        private boolean pollingMessage = false;

    }

}
