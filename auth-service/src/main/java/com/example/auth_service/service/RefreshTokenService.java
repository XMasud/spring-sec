package com.example.auth_service.service;

import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.User;
import com.example.auth_service.exceptions.InvalidRefreshTokenException;
import com.example.auth_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-ttl-days:30}")
    private long refreshTokenTtlDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createRefreshToken(User user){

        String rawToken = generateSecureToken();

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(entity);

        return rawToken;

    }

    public UUID validateAndRotate(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (stored.isRevoked()) {
            // Reuse of a revoked token — revoke everything for this user as a precaution
            refreshTokenRepository.revokeAllForUser(stored.getUserId());
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        refreshTokenRepository.revokeById(stored.getId());
        return stored.getUserId();
    }

    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private String hash(String rawToken) {
        return sha256(rawToken);
    }

    private String generateSecureToken() {

        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
