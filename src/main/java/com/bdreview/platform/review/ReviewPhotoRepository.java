package com.bdreview.platform.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, UUID> {
    List<ReviewPhoto> findByReviewId(UUID reviewId);
    List<ReviewPhoto> findByReviewIdIn(List<UUID> reviewIds); // batch-fetch to avoid N+1 on review lists
}
