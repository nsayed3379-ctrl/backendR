package com.bdreview.platform.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank String phoneNumber, @NotBlank String password) {
}
