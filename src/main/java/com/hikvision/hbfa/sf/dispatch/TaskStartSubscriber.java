package com.hikvision.hbfa.sf.dispatch;

import com.hikvision.hbfa.sf.config.KafkaProperties;
import com.hikvision.hbfa.sf.dto.TaskStartDto;
import com.hikvision.hbfa.sf.handler.dag.DAGTaskService;
import com.hikvision.hbfa.sf.kafka.KafkaRecvHandler;
import com.hikvision.hbfa.sf.kafka.KafkaSubscription;
import com.hikvision.hbfa.sf.util.ParameterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.Valid;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class TaskStartSubscriber implements
        KafkaRecvHandler<String, TaskStartDto> {

    private final KafkaProperties properties;
    private final DAGTaskService dagTaskService;

    public TaskStartSubscriber(KafkaProperties properties,
                               DAGTaskService dagTaskService) {
        this.properties = properties;
        this.dagTaskService = dagTaskService;
    }


    private KafkaSubscription<String, TaskStartDto> subscription;


    @PostConstruct
    public void start() {
        subscription = new KafkaSubscription<>(
                properties.buildConsumerProperties(),
                TaskStartDto.class, this,
                Duration.ofSeconds(1), 3, "TaskRun");
        subscription.updateSubscribe(
                ParameterUtil.TOPIC_TASK_START,
                "");
        subscription.start();
    }


    @Override
    public CompletableFuture<?> receive(String r, @Valid TaskStartDto start) {
        log.debug("run Task of {} by Kafka Subscription", start.getName());
        return dagTaskService.runTask(
                start.getName(), start.getInput(), start.getTimeout());
    }

    @PreDestroy
    public void stop() {
        subscription.stop();
    }

}
