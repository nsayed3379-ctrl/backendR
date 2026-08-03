package com.bdreview.platform.otp;

import com.bdreview.platform.auth.AuthService;
import com.bdreview.platform.auth.TokenPairDto;
import com.bdreview.platform.auth.UserRole;
import com.bdreview.platform.common.BadRequestException;
import com.bdreview.platform.common.PhoneNumberUtils;
import com.bdreview.platform.common.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Spec §6. All limits below are the finalized spec numbers, not placeholders:
 * 5-minute expiry, 5 max verification attempts, 60s resend cooldown,
 * 3 requests/5min and 5 requests/hour per normalized phone number.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpRepository;
    private final SmsGatewayService smsGatewayService;
    private final AuthService authService;

    private final long ttlMinutes;
    private final int maxAttempts;
    private final long resendCooldownSeconds;
    private final long maxPer5Min;
    private final long maxPerHour;

    public OtpService(OtpVerificationRepository otpRepository,
                       SmsGatewayService smsGatewayService,
                       AuthService authService,
                       @Value("${app.otp.ttl-minutes}") long ttlMinutes,
                       @Value("${app.otp.max-attempts}") int maxAttempts,
                       @Value("${app.otp.resend-cooldown-seconds}") long resendCooldownSeconds,
                       @Value("${app.otp.max-requests-per-5-min}") long maxPer5Min,
                       @Value("${app.otp.max-requests-per-hour}") long maxPerHour) {
        this.otpRepository = otpRepository;
        this.smsGatewayService = smsGatewayService;
        this.authService = authService;
        this.ttlMinutes = ttlMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxPer5Min = maxPer5Min;
        this.maxPerHour = maxPerHour;
    }

    @Transactional
    public void requestOtp(String rawPhoneNumber) {
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);
        Instant now = Instant.now();

        if (otpRepository.existsRecentRequest(phone, now.minusSeconds(resendCooldownSeconds))) {
            throw new RateLimitExceededException(
                    "Please wait " + resendCooldownSeconds + "s before requesting another OTP.");
        }
        if (otpRepository.countByPhoneNumberAndCreatedAtAfter(phone, now.minus(5, ChronoUnit.MINUTES)) >= maxPer5Min) {
            throw new RateLimitExceededException("Too many OTP requests — try again in a few minutes.");
        }
        if (otpRepository.countByPhoneNumberAndCreatedAtAfter(phone, now.minus(1, ChronoUnit.HOURS)) >= maxPerHour) {
            throw new RateLimitExceededException("Too many OTP requests this hour — try again later.");
        }

        String code = generateCode();
        otpRepository.save(OtpVerification.builder()
                .phoneNumber(phone)
                .otpHash(hash(code))
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .build());

        smsGatewayService.sendOtp(phone, code);
    }

    @Transactional
    public TokenPairDto verifyOtp(String rawPhoneNumber, String code, UserRole roleIfNewAccount) {
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);

        OtpVerification otp = otpRepository
                .findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new BadRequestException("No pending OTP for this phone number"));

        if (otp.isExpired()) {
            throw new BadRequestException("OTP expired — please request a new one.");
        }
        if (otp.getAttempts() >= maxAttempts) {
            throw new BadRequestException("Too many incorrect attempts — please request a new OTP.");
        }
        if (!hash(code).equals(otp.getOtpHash())) {
            otpRepository.incrementAttempts(otp.getId());
            throw new BadRequestException("Incorrect OTP code.");
        }

        otpRepository.markConsumed(otp.getId());
        return authService.registerOrLogin(phone, roleIfNewAccount);
    }

    private static String generateCode() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
