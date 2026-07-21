package com.example.transaction_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transactionsRaw() {
        return TopicBuilder.name("transactions.raw")
                .partitions(6)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }
}
