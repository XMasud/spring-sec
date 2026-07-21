package com.example.transaction_service.controller;

import com.example.transaction_service.dto.TransactionRequest;
import com.example.transaction_service.dto.TransactionResponse;
import com.example.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody TransactionRequest request) {

        UUID userId = UUID.fromString(userIdHeader);
        TransactionResponse response = transactionService.createTransaction(userId, idempotencyKey, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
