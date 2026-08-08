package com.bdreview.platform.admin.service;

import com.bdreview.platform.business.BusinessRepository;
import com.bdreview.platform.common.ResourceNotFoundException;
import com.bdreview.platform.moderation.AuditLogService;
import com.bdreview.platform.review.Review;
import com.bdreview.platform.review.ReviewRepository;
import com.bdreview.platform.review.VisibilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin-panel review listing/removal. Visibility overrides (approve / flag /
 * hide) go through the existing {@code moderation.ModerationService}
 * unchanged — this class only adds the plain "any review, any status"
 * listing and the admin hard-moderation delete that service doesn't cover.
 */
@Service
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final BusinessRepository businessRepository;
    private final AuditLogService auditLogService;

    public AdminReviewService(ReviewRepository reviewRepository,
                               BusinessRepository businessRepository,
                               AuditLogService auditLogService) {
        this.reviewRepository = reviewRepository;
        this.businessRepository = businessRepository;
        this.auditLogService = auditLogService;
    }

    public Page<Review> search(VisibilityStatus status, int page) {
        return reviewRepository.adminSearch(status, PageRequest.of(page, 20));
    }

    public Review get(UUID id) {
        return reviewRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    /** Mirrors the aggregate-reconciliation in {@code review.ReviewService#delete}, without the ownership check. */
    @Transactional
    public void delete(UUID reviewId, UUID adminId, String notes) {
        Review review = get(reviewId);
        reviewRepository.softDelete(reviewId, Instant.now());
        if (review.getVisibilityStatus() == VisibilityStatus.RECOMMENDED) {
            businessRepository.applyRatingAggregateDelta(review.getBusinessId(), -review.getRating(), -1);
        }
        auditLogService.log("REVIEW", reviewId, "DELETED", adminId, notes);
    }
}
