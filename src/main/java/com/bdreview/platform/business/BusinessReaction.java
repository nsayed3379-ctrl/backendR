package com.bdreview.platform.business;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One reaction per (business, user, type) — enforced by a unique constraint,
 * mirroring {@code review.ReviewVote}'s one-vote-per-type pattern so a user
 * can independently toggle LIKE, DISLIKE, LOVE and WOW on the same listing.
 */
@Entity
@Table(name = "business_reaction", uniqueConstraints =
@UniqueConstraint(columnNames = {"business_id", "user_id", "reaction_type"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessReaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 10)
    private BusinessReactionType reactionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
