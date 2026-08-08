package com.bdreview.platform.claim;

import jakarta.validation.constraints.NotNull;

public record ResolveClaimRequest(@NotNull boolean approve, String notes) {
}
