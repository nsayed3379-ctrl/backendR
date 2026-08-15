package com.bdreview.platform.claim;

import jakarta.validation.constraints.NotBlank;

public record RequestEmailClaimRequest(@NotBlank String email) {
}
