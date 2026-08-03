package com.bdreview.platform.business;

import java.math.BigDecimal;
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
        double latitude,
        double longitude,
        PriceTier priceTier,
        List<String> attributes,
        boolean verified,
        BigDecimal averageRating,
        int reviewCount
) {
    public static BusinessResponse from(Business b) {
        return new BusinessResponse(
                b.getId(), b.getName(), b.getSlug(),
                b.getCategory().getName(), b.getCity().getName(), b.getArea().getName(),
                b.getContactNumber(), b.getOperatingHours(), b.getDescription(), b.getCoverPhotoUrl(),
                b.getLocation().getY(), b.getLocation().getX(),
                b.getPriceTier(),
                b.getAttributes().stream().map(BusinessAttribute::getName).toList(),
                b.isVerified(), b.getAverageRating(), b.getReviewCount()
        );
    }
}
