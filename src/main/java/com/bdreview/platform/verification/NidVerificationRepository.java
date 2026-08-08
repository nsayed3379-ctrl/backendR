package com.bdreview.platform.verification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NidVerificationRepository extends JpaRepository<NidVerification, UUID> {

    /** Admin moderation queue listing (spec §12). */
    Page<NidVerification> findByStatus(NidVerificationStatus status, Pageable pageable);

    List<NidVerification> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE NidVerification n
            SET n.status = :status, n.resolvedByAdminId = :adminId, n.resolvedAt = :now,
                n.imagePurgeAt = :purgeAt
            WHERE n.id = :id
            """)
    void resolve(@Param("id") UUID id,
                 @Param("status") NidVerificationStatus status,
                 @Param("adminId") UUID adminId,
                 @Param("now") Instant now,
                 @Param("purgeAt") Instant purgeAt);

    /** Scheduled data-minimization job picks these up for raw-image deletion. */
    List<NidVerification> findByImagePurgeAtBeforeAndStatusNot(Instant cutoff, NidVerificationStatus excluded);
}
