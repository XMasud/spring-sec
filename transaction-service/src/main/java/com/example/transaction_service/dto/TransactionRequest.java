package com.example.transaction_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank String sourceAccountId,
        @NotBlank String targetAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank String deviceFingerprint,
        @NotBlank String ipAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        @NotBlank String paymentChannel
) {
}
