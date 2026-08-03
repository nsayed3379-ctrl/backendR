package com.bdreview.platform.claim;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileClaimRequest(@NotNull UUID businessId, @NotNull VerificationMethod verificationMethod) {
}
