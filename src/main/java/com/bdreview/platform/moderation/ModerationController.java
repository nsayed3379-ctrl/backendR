package com.bdreview.platform.moderation;

import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.PageResponse;
import com.bdreview.platform.review.Review;
import com.bdreview.platform.review.VisibilityStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Spec §12 basic admin dashboard: combined view over reports/NID queue/flagged-reviews + audit trail. */
@RestController
@RequestMapping("/api/v1/admin/moderation")
public class ModerationController {

    private final ModerationService moderationService;
    private final AuditLogRepository auditLogRepository;

    public ModerationController(ModerationService moderationService, AuditLogRepository auditLogRepository) {
        this.moderationService = moderationService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<ModerationQueueCounts> summary() {
        CurrentUser.requireRole("ADMIN");
        return ResponseEntity.ok(moderationService.counts());
    }

    @GetMapping("/flagged-reviews")
    public ResponseEntity<PageResponse<Review>> flaggedReviews(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        CurrentUser.requireRole("ADMIN");
        return ResponseEntity.ok(PageResponse.of(moderationService.flaggedReviews(PageRequest.of(page, size))));
    }

    /** Admin: hide or restore a flagged review directly from the moderation queue. */
    @PostMapping("/flagged-reviews/{reviewId}/resolve")
    public ResponseEntity<Void> resolveFlaggedReview(@PathVariable UUID reviewId,
                                                       @RequestParam VisibilityStatus resolution,
                                                       @RequestParam(required = false) String notes) {
        CurrentUser.requireRole("ADMIN");
        moderationService.resolveFlaggedReview(reviewId, resolution, CurrentUser.id(), notes);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-log")
    public ResponseEntity<PageResponse<AuditLog>> auditLog(
            @RequestParam String entityType, @RequestParam UUID entityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        CurrentUser.requireRole("ADMIN");
        return ResponseEntity.ok(PageResponse.of(
                auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, PageRequest.of(page, size))));
    }
}
