package com.example.transaction_service.components;

import com.example.transaction_service.entity.TransactionStatus;
import com.example.transaction_service.event.FraudAlertEvent;
import com.example.transaction_service.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionStatusListener {

    private final TransactionRepository transactionRepository;

    // 1. Cleared topic receives plain String UUIDs
    @KafkaListener(
            topics = "transactions.cleared",
            groupId = "transaction-service-status-group",
            properties = {
                    "spring.deserializer.value.delegate.class=org.apache.kafka.common.serialization.StringDeserializer"
            }
    )
    @Transactional
    public void handleClearedTransaction(String transactionIdStr, Acknowledgment ack) {
        log.info("Received cleared confirmation for transactionId: {}", transactionIdStr);

        try {
            // Strip any quotes if present just in case
            String cleanId = transactionIdStr.replace("\"", "").trim();
            UUID transactionId = UUID.fromString(cleanId);

            transactionRepository.findById(transactionId).ifPresent(txn -> {
                if (txn.getStatus() == TransactionStatus.PENDING) {
                    txn.setStatus(TransactionStatus.APPROVED);
                    transactionRepository.save(txn);
                    log.info("Transaction {} updated to APPROVED", transactionId);
                }
            });
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID string received on transactions.cleared: {}", transactionIdStr, e);
        }
        ack.acknowledge();
    }

    // 2. Flagged topic receives JSON FraudAlertEvent objects
    @KafkaListener(
            topics = "transactions.flagged",
            groupId = "transaction-service-status-group",
            properties = {
                    "spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
                    "spring.json.value.default.type=com.example.transaction_service.event.FraudAlertEvent"
            }
    )
    @Transactional
    public void handleFlaggedTransaction(FraudAlertEvent alert, Acknowledgment ack) {
        log.warn("Received fraud alert: [{}] for account: {}", alert.ruleType(), alert.sourceAccountId());

        if (alert.transactionId() != null) {
            transactionRepository.findById(alert.transactionId()).ifPresent(txn -> {
                if (txn.getStatus() == TransactionStatus.PENDING) {
                    txn.setStatus(TransactionStatus.SUSPENDED);
                    txn.setRiskScore(alert.riskScore());
                    transactionRepository.save(txn);
                    log.info("Transaction {} updated to SUSPENDED", txn.getId());
                }
            });
        }
        ack.acknowledge();
    }
}
