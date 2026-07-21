package com.example.transaction_service.dto;

import com.example.transaction_service.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency,
        String status,
        BigDecimal riskScore,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction txn) {
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

