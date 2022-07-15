package com.hikvision.hbfa.sf.dispatch.kafka;

import com.hikvision.hbfa.sf.config.KafkaProperties;
import com.hikvision.hbfa.sf.dispatch.SourceParam;
import com.hikvision.hbfa.sf.dispatch.TaskSourceDispatcher;
import com.hikvision.hbfa.sf.dispatch.TaskSourceDispatcherFactory;
import com.hikvision.hbfa.sf.dispatch.TaskSourceReceiver;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceType;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ValueUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class KafkaTaskSourceDispatcherFactory implements TaskSourceDispatcherFactory {

    final Map<String, Object> defaultProperties;

    public KafkaTaskSourceDispatcherFactory(KafkaProperties properties) {
        this.defaultProperties = properties.buildConsumerProperties();
    }

    @Override
    public TaskSourceType type() {
        return TaskSourceType.KAFKA;
    }

    @Override
    public TaskSourceDispatcher create(
            SourceParam param,
            TaskSourceReceiver receiver) {
        var properties = new HashMap<>(defaultProperties);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, param.getBatch());
        var config = JsonUtil.fromJson(param.getConfig(), KafkaSourceConfig.class);
        if (ValueUtil.isEmpty(config.getTopic())) {
            throw new IllegalArgumentException("topic empty");
        }

        if (!ValueUtil.isEmpty(config.getGroupId())) {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId());
        }

        return new KafkaTaskSourceDispatcher(param, properties, config.getTopic(), receiver);
    }

}
