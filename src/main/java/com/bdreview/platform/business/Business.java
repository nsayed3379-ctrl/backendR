package com.bdreview.platform.business;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Core listing entity (spec §1). Soft-deleted (never hard-deleted) so admins retain
 * access for disputes/investigations. Rating aggregates (averageRating/reviewCount)
 * are written only via atomic SQL increments in {@link BusinessRepository} — never
 * via read-modify-write on this entity — to avoid colliding with the optimistic-lock
 * {@code version} column under concurrent updates.
 */
@Entity
@Table(name = "business")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Business {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    /** Generated at creation, immutable thereafter. On collision, a random suffix is appended. */
    @Column(nullable = false, length = 280)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber; // E.164 normalized

    @Column(name = "operating_hours", columnDefinition = "text")
    private String operatingHours;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "cover_photo_url", columnDefinition = "text")
    private String coverPhotoUrl;

    /** PostGIS geography(Point,4326), GiST-indexed — see V1__init.sql. */
    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_tier", nullable = false, length = 20)
    private PriceTier priceTier;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "business_attribute_assignment",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "attribute_id")
    )
    private Set<BusinessAttribute> attributes = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;

    @Builder.Default
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    /** Backs the atomic average-rating recompute in {@link BusinessRepository}; not surfaced to clients. */
    @Builder.Default
    @Column(name = "rating_sum", nullable = false)
    private int ratingSum = 0;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
