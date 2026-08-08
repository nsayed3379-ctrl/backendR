package com.bdreview.platform.claim;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Owner-claims-existing-listing flow (spec §2). */
@Entity
@Table(name = "business_claim")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessClaim {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "claimant_user_id", nullable = false)
    private UUID claimantUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, length = 20)
    private VerificationMethod verificationMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
