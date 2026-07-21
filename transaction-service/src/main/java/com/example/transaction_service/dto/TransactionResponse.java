package com.example.transaction_service.dto;

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
}
