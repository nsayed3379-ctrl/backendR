package com.bdreview.platform.otp;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Spec §6: OTP expires in 5 minutes, max 5 verification attempts, 60s resend
 * cooldown, stored hashed (never plaintext). Rate limiting (3 requests / 5 min,
 * 5 requests / hour per normalized phone number) is enforced via the counting
 * queries in {@link OtpVerificationRepository}.
 */
@Entity
@Table(name = "otp_verification")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OtpVerification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean consumed = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
