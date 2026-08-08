package com.bdreview.platform.review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id, UUID businessId, UUID userId, short rating, String content,
        VisibilityStatus visibilityStatus, int usefulCount, int funnyCount, int coolCount,
        boolean editable, List<String> photoUrls, Instant createdAt
) {
    public static ReviewResponse from(Review r, List<String> photoUrls) {
        return new ReviewResponse(r.getId(), r.getBusinessId(), r.getUserId(), r.getRating(), r.getContent(),
                r.getVisibilityStatus(), r.getUsefulCount(), r.getFunnyCount(), r.getCoolCount(),
                r.isWithinEditWindow(), photoUrls, r.getCreatedAt());
    }
}
