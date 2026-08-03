package com.bdreview.platform.otp;

import jakarta.validation.constraints.NotBlank;

public record OtpRequestDto(@NotBlank String phoneNumber) {
}
