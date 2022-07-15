package com.hikvision.hbfa.sf.kafka;

import com.hikvision.hbfa.sf.config.KafkaProperties;
import com.hikvision.hbfa.sf.util.ValueUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KafkaPublisher {

    private final KafkaProducer<String, Object> kafkaProducer;

    public KafkaPublisher(KafkaProperties properties) {
        var props = properties.buildProducerProperties();

        kafkaProducer = new KafkaProducer<>(props);
    }


    public CompletableFuture<RecordMetadata> send(String topic, Object value) {
        return send(topic, value, null);
    }

    public CompletableFuture<RecordMetadata> send(String topic, Object value, Map<String, String> headers) {
        var record = new ProducerRecord<String, Object>(topic, null, value);
        if (!ValueUtil.isEmpty(headers)) {
            var rh = record.headers();
            for (var entry : headers.entrySet()) {
                rh.add(entry.getKey(), entry.getValue().getBytes());
            }
        }
        var future = new CompletableFuture<RecordMetadata>();
        kafkaProducer.send(record, (metadata, ex) -> {
            if (ex == null) {
                future.complete(metadata);
            } else {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }


}
