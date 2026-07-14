package com.example.auth_service.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName
) {
}
