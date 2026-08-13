package com.bdreview.platform.business;

import com.bdreview.platform.review.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BusinessReactionRepository extends JpaRepository<BusinessReaction, UUID> {

    boolean existsByBusinessIdAndUserIdAndReactionType(UUID businessId, UUID userId, VoteType reactionType);

    void deleteByBusinessIdAndUserIdAndReactionType(UUID businessId, UUID userId, VoteType reactionType);

    /**
     * Batched per-type counts for a page of businesses (search/mine results) —
     * one query instead of one per business. Row shape: [UUID businessId, VoteType reactionType, Long count].
     */
    @Query("""
            SELECT r.businessId, r.reactionType, COUNT(r)
            FROM BusinessReaction r
            WHERE r.businessId IN :businessIds
            GROUP BY r.businessId, r.reactionType
            """)
    List<Object[]> countGroupedByBusinessIds(@Param("businessIds") Collection<UUID> businessIds);
}
