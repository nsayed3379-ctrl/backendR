package com.bdreview.platform.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyPhoneClaimRequest(@NotNull UUID businessId, @NotBlank String code) {
}
