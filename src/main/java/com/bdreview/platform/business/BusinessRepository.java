package com.bdreview.platform.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    // -----------------------------------------------------------------
    // §2 Business Claim Flow — duplicate check.
    // pg_trgm similarity() is trigram-overlap, NOT edit-distance; combined
    // here with fuzzystrmatch's levenshtein() as a secondary signal, per spec.
    // -----------------------------------------------------------------
    @Query(value = """
            SELECT b.* FROM business b
            WHERE b.deleted_at IS NULL
              AND b.category_id = :categoryId
              AND b.area_id = :areaId
              AND similarity(b.name, :name) > 0.85
            ORDER BY similarity(b.name, :name) DESC
            """, nativeQuery = true)
    List<Business> findPotentialDuplicates(@Param("categoryId") UUID categoryId,
                                           @Param("areaId") UUID areaId,
                                           @Param("name") String name);

    @Query(value = """
            SELECT levenshtein(b.name, :name) FROM business b WHERE b.id = :businessId
            """, nativeQuery = true)
    int levenshteinDistanceTo(@Param("businessId") UUID businessId, @Param("name") String name);

    // -----------------------------------------------------------------
    // §3 Search + Filter — combinable filters, GPS "near me" via PostGIS
    // ST_DWithin/ST_Distance against the GiST-indexed geography column.
    // Null parameters are treated as "no filter" (see COALESCE guards).
    // Sort options: rating | distance | newest | most_reviewed.
    // -----------------------------------------------------------------
    @Query(value = """
            SELECT b.*,
                   ST_Distance(b.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS distance_m
            FROM business b
            WHERE b.deleted_at IS NULL
              AND (:categoryId IS NULL OR b.category_id = :categoryId)
              AND (:areaId IS NULL OR b.area_id = :areaId)
              AND (:priceTier IS NULL OR b.price_tier = :priceTier)
              AND (:minRating IS NULL OR b.average_rating >= :minRating)
              AND (:lat IS NULL OR :lng IS NULL OR :radiusMeters IS NULL
                   OR ST_DWithin(b.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters))
            ORDER BY
              CASE WHEN :sort = 'rating'        THEN b.average_rating END DESC NULLS LAST,
              CASE WHEN :sort = 'most_reviewed'  THEN b.review_count END DESC NULLS LAST,
              CASE WHEN :sort = 'newest'         THEN b.created_at END DESC NULLS LAST,
              CASE WHEN :sort = 'distance' AND :lat IS NOT NULL
                   THEN ST_Distance(b.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) END ASC NULLS LAST
            """,
            countQuery = """
            SELECT count(*) FROM business b
            WHERE b.deleted_at IS NULL
              AND (:categoryId IS NULL OR b.category_id = :categoryId)
              AND (:areaId IS NULL OR b.area_id = :areaId)
              AND (:priceTier IS NULL OR b.price_tier = :priceTier)
              AND (:minRating IS NULL OR b.average_rating >= :minRating)
              AND (:lat IS NULL OR :lng IS NULL OR :radiusMeters IS NULL
                   OR ST_DWithin(b.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters))
            """,
            nativeQuery = true)
    Page<Business> search(@Param("categoryId") UUID categoryId,
                          @Param("areaId") UUID areaId,
                          @Param("priceTier") String priceTier,
                          @Param("minRating") Double minRating,
                          @Param("lat") Double lat,
                          @Param("lng") Double lng,
                          @Param("radiusMeters") Double radiusMeters,
                          @Param("sort") String sort,
                          Pageable pageable);

    /** Filters additionally by required business_attribute ids (owner-declared amenities, spec §1/§3). */
    @Query(value = """
            SELECT b.* FROM business b
            WHERE b.deleted_at IS NULL
              AND b.id IN (
                  SELECT baa.business_id FROM business_attribute_assignment baa
                  WHERE baa.attribute_id IN (:attributeIds)
                  GROUP BY baa.business_id
                  HAVING COUNT(DISTINCT baa.attribute_id) = :attributeCount
              )
            """, nativeQuery = true)
    Page<Business> filterByAllAttributes(@Param("attributeIds") List<UUID> attributeIds,
                                         @Param("attributeCount") long attributeCount,
                                         Pageable pageable);

    // -----------------------------------------------------------------
    // §1 Rating aggregates — atomic SQL increments only, never
    // read-modify-write, so concurrent review writes cannot collide with
    // the entity's optimistic-lock `version`. `ratingDelta`/`countDelta`
    // may be negative (review edited down, or hidden/soft-deleted).
    // -----------------------------------------------------------------
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE business
            SET rating_sum = rating_sum + :ratingDelta,
                review_count = review_count + :countDelta,
                average_rating = CASE
                    WHEN review_count + :countDelta <= 0 THEN 0
                    ELSE ROUND((rating_sum + :ratingDelta)::numeric / (review_count + :countDelta), 2)
                END,
                updated_at = now()
            WHERE id = :businessId
            """, nativeQuery = true)
    void applyRatingAggregateDelta(@Param("businessId") UUID businessId,
                                   @Param("ratingDelta") int ratingDelta,
                                   @Param("countDelta") int countDelta);

    // -----------------------------------------------------------------
    // §1 Soft delete — row stays archived for admin/dispute access.
    // -----------------------------------------------------------------
    @Modifying
    @Transactional
    @Query("UPDATE Business b SET b.deletedAt = CURRENT_TIMESTAMP WHERE b.id = :id AND b.deletedAt IS NULL")
    int softDelete(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Business b SET b.ownerUserId = :newOwnerUserId WHERE b.id = :id")
    void updateOwner(@Param("id") UUID id, @Param("newOwnerUserId") UUID newOwnerUserId);

    @Modifying
    @Transactional
    @Query("UPDATE Business b SET b.verified = true WHERE b.id = :id")
    void markVerified(@Param("id") UUID id);

    /** Report workflow: a LISTING report resolved ACTION_TAKEN flags the business with its reason. */
    @Modifying
    @Transactional
    @Query("UPDATE Business b SET b.flagged = true, b.flagReason = :reason, b.flaggedAt = :now WHERE b.id = :id")
    void flag(@Param("id") UUID id, @Param("reason") String reason, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE Business b SET b.flagged = false, b.flagReason = NULL, b.flaggedAt = NULL WHERE b.id = :id")
    void unflag(@Param("id") UUID id);

    List<Business> findByOwnerUserIdAndDeletedAtIsNull(UUID ownerUserId);

    // -----------------------------------------------------------------
    // Admin panel (com.bdreview.platform.admin) — listing/search across
    // ALL businesses (including soft-deleted, so admins can restore them),
    // plus the verified-toggle and undelete actions the public API never
    // needed. The consumer-facing search()/filter methods above are
    // untouched.
    // -----------------------------------------------------------------
    // JOIN FETCH is required here: spring.jpa.open-in-view is false in this
    // project, so the Hibernate session is closed before Thymeleaf renders
    // the admin list page — without an eager fetch, b.category/b.city
    // (LAZY by default) throw LazyInitializationException at render time.
    // countQuery is given explicitly since Spring Data can't safely derive
    // a count query from a query containing JOIN FETCH.
    @Query(value = """
            SELECT b FROM Business b
            JOIN FETCH b.category
            JOIN FETCH b.city
            JOIN FETCH b.area
            WHERE (:query IS NULL OR :query = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:categoryId IS NULL OR b.category.id = :categoryId)
              AND (:cityId IS NULL OR b.city.id = :cityId)
              AND (:includeDeleted = true OR b.deletedAt IS NULL)
            """,
            countQuery = """
            SELECT count(b) FROM Business b
            WHERE (:query IS NULL OR :query = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:categoryId IS NULL OR b.category.id = :categoryId)
              AND (:cityId IS NULL OR b.city.id = :cityId)
              AND (:includeDeleted = true OR b.deletedAt IS NULL)
            """)
    Page<Business> adminSearch(@Param("query") String query,
                               @Param("categoryId") UUID categoryId,
                               @Param("cityId") UUID cityId,
                               @Param("includeDeleted") boolean includeDeleted,
                               Pageable pageable);

    // Same reasoning as adminSearch above — the business detail/edit pages
    // read category/city/area/attributes, all LAZY, after the session closes.
    @Query("""
            SELECT b FROM Business b
            JOIN FETCH b.category
            JOIN FETCH b.city
            JOIN FETCH b.area
            LEFT JOIN FETCH b.attributes
            WHERE b.id = :id
            """)
    Optional<Business> findByIdWithDetails(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Business b SET b.deletedAt = NULL WHERE b.id = :id")
    void restore(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Business b SET b.verified = :verified WHERE b.id = :id")
    void setVerified(@Param("id") UUID id, @Param("verified") boolean verified);
}
