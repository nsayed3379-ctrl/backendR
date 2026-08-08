package com.bdreview.platform.summary;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessReviewSummaryRepository extends JpaRepository<BusinessReviewSummary, UUID> {

    Optional<BusinessReviewSummary> findByBusinessId(UUID businessId);

    /**
     * Pessimistic row lock: when a burst of reviews crosses the "10 new" threshold
     * concurrently, only the transaction holding this lock proceeds to regenerate
     * the summary — the rest see the already-updated row and no-op.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BusinessReviewSummary s WHERE s.businessId = :businessId")
    Optional<BusinessReviewSummary> findByBusinessIdForUpdate(@Param("businessId") UUID businessId);
}
