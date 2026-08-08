package com.bdreview.platform.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** encryptedImageRef is produced upstream by the gallery/upload flow's KMS-backed storage step. */
public record SubmitNidRequest(@NotNull UUID businessId, @NotBlank String encryptedImageRef) {
}
