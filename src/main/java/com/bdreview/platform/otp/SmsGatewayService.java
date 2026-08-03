package com.bdreview.platform.otp;

/**
 * Spec §6: pluggable interface so any local SMS provider (SSL Wireless, etc.)
 * can be swapped in later without touching OtpService.
 */
public interface SmsGatewayService {
    void sendOtp(String normalizedPhoneNumber, String otpCode);
}
