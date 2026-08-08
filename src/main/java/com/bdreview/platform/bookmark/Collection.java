package com.bdreview.platform.bookmark;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Spec §9. `name == null` represents the default/unnamed "My Favorites" bucket. */
@Entity
@Table(name = "collection")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Collection {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 150)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
