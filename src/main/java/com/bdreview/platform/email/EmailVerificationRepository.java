package com.bdreview.platform.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, Instant since);

    @Query("SELECT COUNT(e) > 0 FROM EmailVerification e WHERE e.email = :email AND e.createdAt > :since")
    boolean existsRecentRequest(@Param("email") String email, @Param("since") Instant since);

    @Modifying
    @Transactional
    @Query("UPDATE EmailVerification e SET e.attempts = e.attempts + 1 WHERE e.id = :id")
    void incrementAttempts(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE EmailVerification e SET e.consumed = true WHERE e.id = :id")
    void markConsumed(@Param("id") UUID id);
}
