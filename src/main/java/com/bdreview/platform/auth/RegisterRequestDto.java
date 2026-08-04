package com.bdreview.platform.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequestDto(
        @NotBlank String phoneNumber,
        @NotBlank String code,
        @NotBlank String password,
        @NotNull UserRole role) {
}
