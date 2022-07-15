package com.hikvision.hbfa.sf.dispatch.kafka;

import com.hikvision.hbfa.sf.dispatch.SourceParam;
import com.hikvision.hbfa.sf.dispatch.TaskSourceDispatcher;
import com.hikvision.hbfa.sf.dispatch.TaskSourceReceiver;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceStatus;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.kafka.KafkaRecvHandler;
import com.hikvision.hbfa.sf.kafka.KafkaSubscription;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class KafkaTaskSourceDispatcher implements
        TaskSourceDispatcher,
        KafkaRecvHandler<String, ObjectMap> {

    private final SourceParam param;
    private final KafkaSubscription<String, ObjectMap> subscription;

    private final TaskSourceReceiver receiver;

    public KafkaTaskSourceDispatcher(
            SourceParam param,
            Map<String, Object> props,
            String topic,
            TaskSourceReceiver receiver) {
        this.param = param;
        subscription = new KafkaSubscription<>(
                props,
                ObjectMap.class, this,
                Duration.ofSeconds(1),
                param.getRetries(),
                "TaskRun-" + param.getName());
        subscription.updateSubscribe(topic, "");
        this.receiver = receiver;
    }

    @Override
    public void start() {
        subscription.start();
    }


    @Override
    public CompletableFuture<Void> stop() {
        return subscription.stop();
    }

    @Override
    public TaskSourceStatus status() {
        return subscription.status();
    }

    @Override
    public CompletableFuture<?> receive(String p, ObjectMap input) {
        return receiver.receive(param, input);
    }

}
