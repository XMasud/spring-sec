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

    @Scheduled(fixedDelay = 500) // poll every 500ms
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository
                .findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send("transactions.raw", event.getPartitionKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS); // synchronous wait — simple and correct for a poller batch

                event.setProcessed(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                // Leave processed=false — next poll cycle retries automatically.
                // Log it; don't let one bad event stop the whole batch.
                log.error("Failed to publish outbox event {}", event.getId(), e);
            }
        }
    }
}
