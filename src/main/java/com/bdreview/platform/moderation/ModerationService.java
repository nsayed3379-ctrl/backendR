package com.bdreview.platform.moderation;

import com.bdreview.platform.business.BusinessRepository;
import com.bdreview.platform.common.ResourceNotFoundException;
import com.bdreview.platform.report.ReportRepository;
import com.bdreview.platform.report.ReportStatus;
import com.bdreview.platform.review.Review;
import com.bdreview.platform.review.ReviewRepository;
import com.bdreview.platform.review.VisibilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Spec §12: read-side aggregation over the moderation-relevant queues
 * (reports, medium-confidence fake-review flags), plus the admin override
 * action for a flagged review. Each queue's own resolve action (claims,
 * reports) stays in its own package/service — this class only composes the
 * "what needs an admin's attention" view and the one action (review
 * visibility override) that doesn't already belong anywhere else.
 */
@Service
public class ModerationService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final BusinessRepository businessRepository;
    private final AuditLogService auditLogService;

    public ModerationService(ReportRepository reportRepository,
                              ReviewRepository reviewRepository,
                              BusinessRepository businessRepository,
                              AuditLogService auditLogService) {
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.businessRepository = businessRepository;
        this.auditLogService = auditLogService;
    }

    public ModerationQueueCounts counts() {
        long reports = reportRepository.findByStatus(ReportStatus.PENDING, Pageable.unpaged()).getTotalElements();
        long flagged = reviewRepository.findByVisibilityStatusAndDeletedAtIsNull(
                VisibilityStatus.NOT_RECOMMENDED, Pageable.unpaged()).getTotalElements();
        return new ModerationQueueCounts(reports, flagged);
    }

    public Page<Review> flaggedReviews(Pageable pageable) {
        return reviewRepository.findByVisibilityStatusAndDeletedAtIsNull(VisibilityStatus.NOT_RECOMMENDED, pageable);
    }

    /** Admin override: hide or restore a flagged review, reconciling the rating aggregate accordingly. */
    @Transactional
    public void resolveFlaggedReview(UUID reviewId, VisibilityStatus resolution, UUID adminId, String notes) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        VisibilityStatus previous = review.getVisibilityStatus();
        reviewRepository.applyFakeReviewVerdict(reviewId, review.getSuspicionScore(), resolution);

        boolean wasCounted = previous == VisibilityStatus.RECOMMENDED;
        boolean nowCounted = resolution == VisibilityStatus.RECOMMENDED;
        if (wasCounted && !nowCounted) {
            businessRepository.applyRatingAggregateDelta(review.getBusinessId(), -review.getRating(), -1);
        } else if (!wasCounted && nowCounted) {
            businessRepository.applyRatingAggregateDelta(review.getBusinessId(), review.getRating(), 1);
        }

        auditLogService.log("REVIEW", reviewId, resolution.name(), adminId, notes);
    }
}
