package com.example.transaction_service.service;

import com.example.transaction_service.dto.TransactionCreatedEvent;
import com.example.transaction_service.dto.TransactionRequest;
import com.example.transaction_service.dto.TransactionResponse;
import com.example.transaction_service.entity.OutboxEvent;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.entity.TransactionMetadata;
import com.example.transaction_service.repository.OutboxEventRepository;
import com.example.transaction_service.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionResponse createTransaction(UUID userId, String idempotencyKey, TransactionRequest request){

        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Transaction txn = getTransaction(userId, idempotencyKey, request);

        transactionRepository.save(txn);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(txn.getId().toString());
        event.setPartitionKey(txn.getSourceAccountId()); // fraud rules key on the SOURCE account
        event.setEventType("TRANSACTION_CREATED");
        event.setPayload(serializePayload(txn));
        outboxEventRepository.save(event);

        return toResponse(txn);

    }

    private static @NonNull Transaction getTransaction(UUID userId, String idempotencyKey, TransactionRequest request) {
        Transaction txn = new Transaction();
        txn.setUserId(userId);
        txn.setSourceAccountId(request.sourceAccountId());
        txn.setTargetAccountId(request.targetAccountId());
        txn.setAmount(request.amount());
        txn.setCurrency(request.currency());
        txn.setStatus("PENDING");
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

    private String serializePayload(Transaction txn) {
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
                txn.getStatus(),
                txn.getRiskScore(),
                txn.getCreatedAt()
        );
    }
}
