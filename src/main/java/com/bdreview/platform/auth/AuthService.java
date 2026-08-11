package com.bdreview.platform.auth;

import com.bdreview.platform.common.BadRequestException;
import com.bdreview.platform.common.ForbiddenException;
import com.bdreview.platform.common.PhoneNumberUtils;
import com.bdreview.platform.otp.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Handles registration, phone+password login, password reset, and
 * token issuance/rotation per spec §5. Registration and password reset both
 * delegate phone-ownership proof to otp.OtpService; plain login needs no OTP.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PASSWORD_MIN_LENGTH = 8;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlDays;

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        JwtService jwtService,
                        OtpService otpService,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
                        @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    /**
     * Creates a new account. Requires a phone number to have just cleared
     * OTP verification (spec §6) and a role other than ADMIN (§5: admin
     * accounts cannot be self-registered). Auto-logs-in on success.
     */
    @Transactional
    public TokenPairDto register(String rawPhoneNumber, String code, String password, UserRole role, String name) {
        if (role == null || role == UserRole.ADMIN) {
            throw new BadRequestException("Invalid role for self-registration.");
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required.");
        }
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);
        validatePassword(password);
        otpService.verifyCode(phone, code);

        if (userRepository.existsByPhoneNumber(phone)) {
            throw new BadRequestException("This phone number is already registered — please log in instead.");
        }

        User user = userRepository.save(User.builder()
                .phoneNumber(phone)
                .role(role)
                .otpVerified(true)
                .passwordHash(passwordEncoder.encode(password))
                .name(name.trim())
                .build());

        return issueNewTokenFamily(user.getId(), user.getRole());
    }

    /** Ordinary phone+password login — no OTP involved. */
    @Transactional
    public TokenPairDto login(String rawPhoneNumber, String password) {
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);
        User user = userRepository.findByPhoneNumber(phone).orElse(null);

        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Invalid phone number or password.");
        }

        return issueNewTokenFamily(user.getId(), user.getRole());
    }

    /**
     * Sets a new password after proving phone ownership via OTP, then
     * revokes all existing sessions (any refresh token issued under the old
     * password) and logs the user in with a fresh token family.
     */
    @Transactional
    public TokenPairDto resetPassword(String rawPhoneNumber, String code, String newPassword) {
        String phone = PhoneNumberUtils.normalize(rawPhoneNumber);
        validatePassword(newPassword);
        otpService.verifyCode(phone, code);

        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new BadRequestException("No account found for this phone number."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());

        return issueNewTokenFamily(user.getId(), user.getRole());
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            throw new BadRequestException("Password must be at least " + PASSWORD_MIN_LENGTH + " characters.");
        }
    }

    private TokenPairDto issueNewTokenFamily(UUID userId, UserRole role) {
        UUID familyId = UUID.randomUUID();
        return issueToken(userId, role, familyId);
    }

    private TokenPairDto issueToken(UUID userId, UserRole role, UUID familyId) {
        String accessToken = jwtService.generateAccessToken(userId, role);
        String refreshTokenPlain = randomToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .familyId(familyId)
                .tokenHash(hash(refreshTokenPlain))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .build());

        return new TokenPairDto(accessToken, refreshTokenPlain, accessTokenTtlMinutes * 60);
    }

    /**
     * §5 rotation + reuse detection: presenting an already-rotated-out or
     * revoked refresh token is treated as a theft signal — the entire token
     * family is revoked and the caller must re-login from scratch.
     */
    @Transactional
    public TokenPairDto refresh(String refreshTokenPlain) {
        String hash = hash(refreshTokenPlain);

        if (refreshTokenRepository.findReusedToken(hash).isPresent()) {
            RefreshToken reused = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
            refreshTokenRepository.revokeFamily(reused.getFamilyId());
            throw new ForbiddenException("Refresh token reuse detected — please log in again.");
        }

        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ForbiddenException("Invalid refresh token"));

        if (!current.isUsable()) {
            throw new ForbiddenException("Refresh token expired or revoked — please log in again.");
        }

        refreshTokenRepository.markRotated(current.getId(), Instant.now());

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new ForbiddenException("User no longer exists"));

        return issueToken(user.getId(), user.getRole(), current.getFamilyId());
    }

    /** Logout or any caller-detected suspicious activity immediately invalidates all refresh tokens. */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
