package com.bdreview.platform.review;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** One vote per (review, user, type) — enforced by a unique constraint (spec §4). */
@Entity
@Table(name = "review_vote", uniqueConstraints =
        @UniqueConstraint(columnNames = {"review_id", "user_id", "vote_type"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewVote {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 10)
    private VoteType voteType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
