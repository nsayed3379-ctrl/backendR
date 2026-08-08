package com.bdreview.platform.bookmark;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookmark", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "business_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Bookmark {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    /** Nullable — unassigned bookmarks fall into the user's default "My Favorites". */
    @Column(name = "collection_id")
    private UUID collectionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
