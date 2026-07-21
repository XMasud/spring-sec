package com.example.fraud_rules_engine.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fraud-rules-engine");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        // Number of stream threads THIS instance runs — separate from partition count.
        // With 6 partitions and 2 threads here, this one instance handles 2 partitions
        // worth of parallelism; run more instances (each auto-splitting partitions via
        // the Kafka Streams group protocol) to use all 6 partitions.
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);

        // Commit interval — how often processed offsets + state store changelogs flush.
        // Lower = less reprocessing on crash, more broker write load. 5s is a common default.
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5000);

        // Exactly-once processing guarantee for the whole topology (read-process-write atomically)
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        return new KafkaStreamsConfiguration(props);
    }
}
