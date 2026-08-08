package com.bdreview.platform.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh tokens are stored hashed and rotated on every use (spec §5).
 * {@code familyId} links every token descended from one login; presenting an
 * already-rotated-out token again is a theft signal — the whole family gets
 * revoked and the user is forced to re-login (see RefreshTokenRepository).
 */
@Entity
@Table(name = "refresh_token")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isUsable() {
        return !revoked && rotatedAt == null && Instant.now().isBefore(expiresAt);
    }
}
