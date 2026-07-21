package com.example.fraud_rules_engine.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID transactionId,
        UUID userId,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency,
        String deviceFingerprint,
        String ipAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String paymentChannel,
        Instant createdAt
) {
}
