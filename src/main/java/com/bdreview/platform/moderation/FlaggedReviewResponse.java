package com.bdreview.platform.moderation;

import com.bdreview.platform.review.Review;
import com.bdreview.platform.review.VisibilityStatus;

import java.time.Instant;
import java.util.UUID;

public record FlaggedReviewResponse(
        UUID id, UUID businessId, UUID userId, String userName, short rating, String content,
        VisibilityStatus visibilityStatus, short suspicionScore, int usefulCount, int funnyCount, int coolCount,
        Instant createdAt
) {
    public static FlaggedReviewResponse from(Review r, String userName) {
        return new FlaggedReviewResponse(r.getId(), r.getBusinessId(), r.getUserId(), userName, r.getRating(),
                r.getContent(), r.getVisibilityStatus(), r.getSuspicionScore(), r.getUsefulCount(),
                r.getFunnyCount(), r.getCoolCount(), r.getCreatedAt());
    }
}
