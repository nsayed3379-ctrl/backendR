package com.bdreview.platform.business;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record CreateBusinessRequest(
        @NotBlank String name,
        @NotNull UUID categoryId,
        @NotNull UUID cityId,
        @NotNull UUID areaId,
        @NotBlank String contactNumber,
        String operatingHours,
        String description,
        String coverPhotoUrl,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull PriceTier priceTier,
        List<UUID> attributeIds
) {
}
