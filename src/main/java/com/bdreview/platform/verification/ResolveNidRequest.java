package com.bdreview.platform.verification;

import jakarta.validation.constraints.NotNull;

public record ResolveNidRequest(@NotNull boolean approve, String notes) {
}
