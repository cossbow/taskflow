package com.hikvision.hbfa.sf.config;

import com.hikvision.hbfa.sf.util.ValueUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.hikvision.hbfa.sf.util.ParameterUtil.*;

@Slf4j
@Component
public class SubmitManager {

    //

    private final String kafkaServers;
    private final String kafkaZookeeper;

    @Value("${spring.application.name}")
    private String serviceId;
    @Value("${server.address:localhost}:${server.port}")
    private String host;
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public SubmitManager(KafkaProperties kafkaProperties, Environment environment) {
        kafkaServers = String.join(",", kafkaProperties.getBootstrapServers());
        kafkaZookeeper = environment.getProperty("spring.kafka.zookeeper");
        log.info("kafka config: bootstrap-servers=[{}] zookeeper=[{}]", kafkaServers, kafkaZookeeper);
    }


    public Map<String, Object> submitArguments(long nodeId, String subtaskId) {
        return Map.of(
                "kafka", Map.of(
                        "servers", ValueUtil.nullIf(kafkaServers, ""),
                        "zookeeper", ValueUtil.nullIf(kafkaZookeeper, ""),
                        "topic", topic4Node(nodeId)
                ), "rest", Map.of(
                        "host", host,
                        "service", serviceId,
                        "path", submitPath(contextPath, subtaskId)
                ));
    }

    public void addSubmitArguments(Map<String, Object> arguments, long nodeId, String subtaskId) {
        log.debug("add async submit arguments for Subtask-{} ", subtaskId);
        arguments.put(KEY_SUBMIT, submitArguments(nodeId, subtaskId));
    }

    public String kafka() {
        return kafkaServers;
    }

    public String zookeeper() {
        return kafkaZookeeper;
    }

}
