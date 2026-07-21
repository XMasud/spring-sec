package com.example.transaction_service.entity;

import java.util.Arrays;

public enum TransactionStatus {

    PENDING("PENDING"),
    APPROVED("APPROVED"),
    DECLINED("DECLINED"),
    SUSPENDED("SUSPENDED");

    private final String code;

    TransactionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TransactionStatus fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(status -> status.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown transaction status: " + code));
    }
}
