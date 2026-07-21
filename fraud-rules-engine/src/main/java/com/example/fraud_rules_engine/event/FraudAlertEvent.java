package com.example.fraud_rules_engine.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertEvent(
        UUID transactionId,
        String sourceAccountId,
        String ruleType,       // "VELOCITY_BREACH", "AMOUNT_SPIKE"
        BigDecimal riskScore,
        String reason,
        Instant flaggedAt
) {
}
