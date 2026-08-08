package com.bdreview.platform.bookmark;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddBookmarkRequest(@NotNull UUID businessId, UUID collectionId) {
}
