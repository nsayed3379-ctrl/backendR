package com.bdreview.platform.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Theft-detection check: was this hash already rotated out or revoked? */
    @Query("""
            SELECT rt FROM RefreshToken rt
            WHERE rt.tokenHash = :tokenHash AND (rt.revoked = true OR rt.rotatedAt IS NOT NULL)
            """)
    Optional<RefreshToken> findReusedToken(@Param("tokenHash") String tokenHash);

    /** Called on rotation: mark the presented token as consumed. */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.rotatedAt = :now WHERE rt.id = :id")
    void markRotated(@Param("id") UUID id, @Param("now") Instant now);

    /** Reuse detected, or explicit logout/suspicious-activity — revoke the whole family. */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId")
    void revokeFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId")
    void revokeAllForUser(@Param("userId") UUID userId);
}
