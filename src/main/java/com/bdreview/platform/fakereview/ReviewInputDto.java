package com.bdreview.platform.fakereview;

import java.time.Instant;
import java.util.UUID;

/** Wire shape sent to the ML service — mirrors app.schemas.ReviewInput on the Python side. */
public record ReviewInputDto(UUID id, UUID businessId, int rating, String content, Instant createdAt) {
}
