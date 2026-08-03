package com.bdreview.platform.otp;

import com.bdreview.platform.auth.UserRole;
import jakarta.validation.constraints.NotBlank;

/** roleIfNewAccount is only used the first time this phone number verifies (spec §5). */
public record OtpVerifyRequestDto(@NotBlank String phoneNumber, @NotBlank String code, UserRole roleIfNewAccount) {
}
