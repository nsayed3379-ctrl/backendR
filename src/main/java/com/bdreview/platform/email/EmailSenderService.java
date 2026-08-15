package com.bdreview.platform.email;

/**
 * Pluggable interface so a real provider (SES, SendGrid, etc.) can be swapped
 * in later without touching {@link EmailVerificationService} — mirrors
 * {@code otp.SmsGatewayService}'s role for the phone channel.
 */
public interface EmailSenderService {
    void sendVerificationCode(String email, String code);
}
