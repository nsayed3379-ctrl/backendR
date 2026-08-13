package com.bdreview.platform.review;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Home page "Recent Activity" feed (spec-adjacent addition, not from a
 * numbered section) — a platform-wide feed of recent reviews, each enriched
 * with just enough business + reviewer context that the frontend can render
 * a card without a second round trip per item.
 */
public record RecentActivityResponse(
        UUID reviewId,
        UUID businessId,
        String businessName,
        String businessSlug,
        String businessCoverPhotoUrl,
        String businessCategoryName,
        UUID userId,
        String userName,
        short rating,
        String content,
        List<String> photoUrls,
        int usefulCount,
        int funnyCount,
        int coolCount,
        Instant createdAt
) {
}
