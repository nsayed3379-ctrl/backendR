package com.bdreview.platform.fakereview;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Individual per-signal contribution recorded alongside the review's aggregate
 * suspicion score, so admins reviewing the moderation queue can see *why* a
 * review was flagged, not just the final number.
 */
@Entity
@Table(name = "fake_review_signal")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FakeReviewSignal {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 30)
    private FakeReviewSignalType signalType;

    @Column(nullable = false)
    private short score;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
