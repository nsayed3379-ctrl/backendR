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
        Instant flaggedAt
) {
    /** photoUrls: cover photo (if any) followed by gallery photos, in display order — card carousel source. */
    public static BusinessResponse from(Business b, List<String> photoUrls) {
        return new BusinessResponse(
                b.getId(), b.getName(), b.getSlug(),
                b.getCategory().getName(), b.getCity().getName(), b.getArea().getName(),
                b.getContactNumber(), b.getOperatingHours(), b.getDescription(), b.getCoverPhotoUrl(),
                b.getLogoUrl(), photoUrls,
                b.getLocation().getY(), b.getLocation().getX(),
                b.getPriceTier(),
                b.getAttributes().stream().map(BusinessAttribute::getName).toList(),
                b.isVerified(), b.getAverageRating(), b.getReviewCount(),
                b.isFlagged(), b.getFlagReason(), b.getFlaggedAt()
        );
    }
}
