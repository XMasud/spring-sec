package com.example.transaction_service.service;

import com.example.transaction_service.dto.TransactionCreatedEvent;
import com.example.transaction_service.dto.TransactionRequest;
import com.example.transaction_service.dto.TransactionResponse;
import com.example.transaction_service.entity.OutboxEvent;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.entity.TransactionMetadata;
import com.example.transaction_service.entity.TransactionStatus;
import com.example.transaction_service.repository.OutboxEventRepository;
import com.example.transaction_service.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionResponse createTransaction(UUID userId, String idempotencyKey, TransactionRequest request) throws JsonProcessingException {

        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent request hit for key: {}. Returning existing transaction.", idempotencyKey);
            return toResponse(existing.get());
        }

        Transaction txn = getTransaction(userId, idempotencyKey, request);
        transactionRepository.save(txn);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(txn.getId().toString());
        event.setPartitionKey(txn.getSourceAccountId()); // fraud rules key on the SOURCE account
        event.setEventType("TRANSACTION_CREATED");
        event.setPayload(serializePayload(txn));

        OutboxEvent savedEvent = outboxEventRepository.save(event);
        log.info("Saved OutboxEvent ID: {} for Transaction ID: {}", savedEvent.getId(), txn.getId()); // 👈 Log verification

        return toResponse(txn);

    }

    private static @NonNull Transaction getTransaction(UUID userId, String idempotencyKey, TransactionRequest request) {
        Transaction txn = new Transaction();
        txn.setUserId(userId);
        txn.setSourceAccountId(request.sourceAccountId());
        txn.setTargetAccountId(request.targetAccountId());
        txn.setAmount(request.amount());
        txn.setCurrency(request.currency());
        txn.setStatus(TransactionStatus.PENDING);
        txn.setIdempotencyKey(idempotencyKey);

        TransactionMetadata metadata = new TransactionMetadata();
        metadata.setTransaction(txn);
        metadata.setDeviceFingerprint(request.deviceFingerprint());
        metadata.setIpAddress(request.ipAddress());
        metadata.setLatitude(request.latitude());
        metadata.setLongitude(request.longitude());
        metadata.setPaymentChannel(request.paymentChannel());
        txn.setMetadata(metadata);
        return txn;
    }

    private String serializePayload(Transaction txn) throws JsonProcessingException {
        return objectMapper.writeValueAsString(
                new TransactionCreatedEvent(
                        txn.getId(), txn.getUserId(), txn.getSourceAccountId(),
                        txn.getTargetAccountId(), txn.getAmount(), txn.getCurrency(),
                        txn.getMetadata().getDeviceFingerprint(), txn.getMetadata().getIpAddress(),
                        txn.getMetadata().getLatitude(), txn.getMetadata().getLongitude(),
                        txn.getMetadata().getPaymentChannel(), txn.getCreatedAt()
                )
        );
    }

    private TransactionResponse toResponse(Transaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getSourceAccountId(),
                txn.getTargetAccountId(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getStatus() != null ? txn.getStatus().getCode() : null,
                txn.getRiskScore(),
                txn.getCreatedAt()
        );
    }
}
