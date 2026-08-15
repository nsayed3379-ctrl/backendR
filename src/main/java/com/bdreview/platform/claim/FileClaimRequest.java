package com.bdreview.platform.claim;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Only the DOCUMENT method files this way — PHONE/EMAIL claim instantly via their own request/verify endpoints. */
public record FileClaimRequest(@NotNull UUID businessId, @NotNull VerificationMethod verificationMethod, String documentRef) {
}
