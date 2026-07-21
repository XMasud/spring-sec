package com.example.transaction_service.components;

import com.ctc.wstx.msv.BaseSchemaFactory;
import com.example.transaction_service.entity.OutboxEvent;
import com.example.transaction_service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000) // Runs every 1 second
    @Transactional
    public void processOutboxEvents() {

        var pendingEvents = outboxEventRepository.findByProcessedFalseOrderByIdAsc();

        if (!pendingEvents.isEmpty()) {
            log.info("Found {} pending outbox events to publish", pendingEvents.size());
        }

        for (var event : pendingEvents) {
            try {
                // Send payload to Kafka topic "transactions.raw"
                kafkaTemplate.send("transactions.raw", event.getPartitionKey(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Successfully published outbox event [{}] to Kafka topic transactions.raw", event.getId());
                            } else {
                                log.error("Failed to publish outbox event [{}] to Kafka", event.getId(), ex);
                            }
                        });

                event.setProcessed(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error dispatching outbox event [{}]", event.getId(), e);
            }
        }
    }
}
