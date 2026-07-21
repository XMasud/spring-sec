package com.example.transaction_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transaction_metadata")
@Getter
@Setter
@NoArgsConstructor
public class TransactionMetadata {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    @Column(name = "ip_address", nullable = false, columnDefinition = "inet")
    private String ipAddress;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "payment_channel", nullable = false)
    private String paymentChannel;
}
