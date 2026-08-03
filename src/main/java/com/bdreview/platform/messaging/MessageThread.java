package com.bdreview.platform.messaging;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Spec §16: a consumer's direct inquiry to a business owner reuses the same
 * messaging channel as owner-reply-to-review (§7) — this is simply the reverse
 * direction of the same thread/infra, not a new subsystem. A thread is scoped
 * to exactly one consumer + one business.
 */
@Entity
@Table(name = "message_thread", uniqueConstraints =
        @UniqueConstraint(columnNames = {"consumer_user_id", "business_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MessageThread {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "consumer_user_id", nullable = false)
    private UUID consumerUserId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
