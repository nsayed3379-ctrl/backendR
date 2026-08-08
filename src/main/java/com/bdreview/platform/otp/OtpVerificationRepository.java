package com.bdreview.platform.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findTopByPhoneNumberAndConsumedFalseOrderByCreatedAtDesc(String phoneNumber);

    /** Rate limit: max 3 requests / 5 min. */
    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant since);

    /** 60s resend cooldown check reuses the same counting query with a 60s window. */
    @Query("SELECT COUNT(o) > 0 FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.createdAt > :since")
    boolean existsRecentRequest(@Param("phoneNumber") String phoneNumber, @Param("since") Instant since);

    @Modifying
    @Transactional
    @Query("UPDATE OtpVerification o SET o.attempts = o.attempts + 1 WHERE o.id = :id")
    void incrementAttempts(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE OtpVerification o SET o.consumed = true WHERE o.id = :id")
    void markConsumed(@Param("id") UUID id);
}
