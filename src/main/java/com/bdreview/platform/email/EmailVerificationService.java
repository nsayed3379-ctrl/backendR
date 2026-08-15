package com.bdreview.platform.email;

import com.bdreview.platform.common.BadRequestException;
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
 * Email-channel counterpart to {@code otp.OtpService} — reuses the same
 * {@code app.otp.*} TTL/attempts/rate-limit policy since the two channels
 * share identical anti-abuse requirements.
 */
@Service
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository repository;
    private final EmailSenderService emailSenderService;

    private final long ttlMinutes;
    private final int maxAttempts;
    private final long resendCooldownSeconds;
    private final long maxPer5Min;
    private final long maxPerHour;

    public EmailVerificationService(EmailVerificationRepository repository,
                                     EmailSenderService emailSenderService,
                                     @Value("${app.otp.ttl-minutes}") long ttlMinutes,
                                     @Value("${app.otp.max-attempts}") int maxAttempts,
                                     @Value("${app.otp.resend-cooldown-seconds}") long resendCooldownSeconds,
                                     @Value("${app.otp.max-requests-per-5-min}") long maxPer5Min,
                                     @Value("${app.otp.max-requests-per-hour}") long maxPerHour) {
        this.repository = repository;
        this.emailSenderService = emailSenderService;
        this.ttlMinutes = ttlMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxPer5Min = maxPer5Min;
        this.maxPerHour = maxPerHour;
    }

    @Transactional
    public void requestCode(String rawEmail) {
        String email = normalize(rawEmail);
        Instant now = Instant.now();

        if (repository.existsRecentRequest(email, now.minusSeconds(resendCooldownSeconds))) {
            throw new RateLimitExceededException(
                    "Please wait " + resendCooldownSeconds + "s before requesting another code.");
        }
        if (repository.countByEmailAndCreatedAtAfter(email, now.minus(5, ChronoUnit.MINUTES)) >= maxPer5Min) {
            throw new RateLimitExceededException("Too many verification requests — try again in a few minutes.");
        }
        if (repository.countByEmailAndCreatedAtAfter(email, now.minus(1, ChronoUnit.HOURS)) >= maxPerHour) {
            throw new RateLimitExceededException("Too many verification requests this hour — try again later.");
        }

        String code = generateCode();
        repository.save(EmailVerification.builder()
                .email(email)
                .otpHash(hash(code))
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .build());

        emailSenderService.sendVerificationCode(email, code);
    }

    @Transactional
    public void verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);

        EmailVerification verification = repository
                .findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("No pending verification for this email"));

        if (verification.isExpired()) {
            throw new BadRequestException("Code expired — please request a new one.");
        }
        if (verification.getAttempts() >= maxAttempts) {
            throw new BadRequestException("Too many incorrect attempts — please request a new code.");
        }
        if (!hash(code).equals(verification.getOtpHash())) {
            repository.incrementAttempts(verification.getId());
            throw new BadRequestException("Incorrect verification code.");
        }

        repository.markConsumed(verification.getId());
    }

    private static String normalize(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        String email = rawEmail.trim().toLowerCase();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BadRequestException("Invalid email address");
        }
        return email;
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
