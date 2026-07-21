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
                .replicas(1) // use 3 with a real multi-broker cluster
                .config("retention.ms", "604800000") // 7 days
                .build();
    }

    @Bean
    public NewTopic transactionsFlagged() {
        return TopicBuilder.name("transactions.flagged").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic transactionsCleared() {
        return TopicBuilder.name("transactions.cleared").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic transactionsRawRetry() {
        return TopicBuilder.name("transactions.raw.retry").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transactionsRawDlq() {
        return TopicBuilder.name("transactions.raw.DLQ").partitions(3).replicas(1).build();
    }
}
