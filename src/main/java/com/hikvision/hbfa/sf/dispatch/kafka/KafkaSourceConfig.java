package com.hikvision.hbfa.sf.dispatch.kafka;

import lombok.Data;

@Data
public class KafkaSourceConfig {
    private String topic;
    private String groupId;
}
