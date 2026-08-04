package com.bdreview.platform.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 120) String name,
        @NotBlank String preferredLanguage,
        String profilePhotoUrl) {
}
