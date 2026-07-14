package com.example.auth_service.repository;

import com.example.auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.id = :id")
    void revokeById(@Param("id") UUID id);
}
