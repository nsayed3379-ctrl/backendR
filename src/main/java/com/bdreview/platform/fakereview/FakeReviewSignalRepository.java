package com.bdreview.platform.fakereview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FakeReviewSignalRepository extends JpaRepository<FakeReviewSignal, UUID> {

    List<FakeReviewSignal> findByReviewId(UUID reviewId);

    /** Batch-fetch signals for a page of moderation-queue reviews (avoids N+1). */
    List<FakeReviewSignal> findByReviewIdIn(List<UUID> reviewIds);
}
