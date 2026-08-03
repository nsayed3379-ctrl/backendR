package com.bdreview.platform.verification;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Spec §8. `encryptedImageRef` is a pointer/key-id into KMS-backed object storage —
 * this table never stores a raw/public URL. On resolution, `imagePurgeAt` schedules
 * data-minimization deletion of the underlying raw image (separate scheduled job).
 */
@Entity
@Table(name = "nid_verification")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NidVerification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "encrypted_image_ref", nullable = false, columnDefinition = "text")
    private String encryptedImageRef;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NidVerificationStatus status = NidVerificationStatus.PENDING;

    @Column(name = "resolved_by_admin_id")
    private UUID resolvedByAdminId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "image_purge_at")
    private Instant imagePurgeAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
