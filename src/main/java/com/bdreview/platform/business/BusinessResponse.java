package com.bdreview.platform.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessResponse(
        UUID id,
        String name,
        String slug,
        String categoryName,
        String cityName,
        String areaName,
        String contactNumber,
        String operatingHours,
        String description,
        String coverPhotoUrl,
        String logoUrl,
        List<String> photoUrls,
        double latitude,
        double longitude,
        PriceTier priceTier,
        List<String> attributes,
        boolean verified,
        BigDecimal averageRating,
        int reviewCount,
        boolean flagged,
        String flagReason,
        Instant flaggedAt,
<<<<<<< HEAD
        int totalLikeCount,
        int totalDislikeCount,
        int totalLoveCount,
        int totalWowCount
=======
        // Counts of direct business-level reactions (business.BusinessReaction) — a user
        // reacting to the business as a whole, distinct from voting on one specific
        // review (see review.ReviewResponse's usefulCount/etc. for that). Live-clickable
        // from the business card via POST /businesses/{id}/react.
        int totalUsefulCount,
        int totalFunnyCount,
        int totalCoolCount
>>>>>>> c75f009dc3431256bd307fac47a70595495e4001
) {
    /** photoUrls: cover photo (if any) followed by gallery photos, in display order — card carousel source. */
    public static BusinessResponse from(Business b, List<String> photoUrls, int[] reactionTotals) {
        return new BusinessResponse(
                b.getId(), b.getName(), b.getSlug(),
                b.getCategory().getName(), b.getCity().getName(), b.getArea().getName(),
                b.getContactNumber(), b.getOperatingHours(), b.getDescription(), b.getCoverPhotoUrl(),
                b.getLogoUrl(), photoUrls,
                b.getLocation().getY(), b.getLocation().getX(),
                b.getPriceTier(),
                b.getAttributes().stream().map(BusinessAttribute::getName).toList(),
                b.isVerified(), b.getAverageRating(), b.getReviewCount(),
                b.isFlagged(), b.getFlagReason(), b.getFlaggedAt(),
<<<<<<< HEAD
                b.getTotalLikeCount(), b.getTotalDislikeCount(), b.getTotalLoveCount(), b.getTotalWowCount()
=======
                reactionTotals[0], reactionTotals[1], reactionTotals[2]
>>>>>>> c75f009dc3431256bd307fac47a70595495e4001
        );
    }
}