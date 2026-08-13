package com.bdreview.platform.business;

import com.bdreview.platform.review.VoteType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One reaction per (business, user, type) — enforced by a unique constraint.
 * Distinct from review.ReviewVote: this reacts to the business as a whole
 * (shown on business cards), not to any single review's content.
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
    private VoteType reactionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
