package com.hikvision.hbfa.sf.config;

import com.hikvision.hbfa.sf.kafka.KafkaPublisher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(KafkaProperties.class)
@Configuration
public class KafkaConfiguration {

    @Bean
    KafkaPublisher kafkaPublisher(KafkaProperties properties) {
        return new KafkaPublisher(properties);
    }

}
