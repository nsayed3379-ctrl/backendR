package com.bdreview.platform.gallery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmUploadRequest(@NotNull UUID businessId, @NotBlank String cdnUrl) {
}
