package com.bdreview.platform.auth;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDto(
        @NotBlank String phoneNumber,
        @NotBlank String code,
        @NotBlank String newPassword) {
}
