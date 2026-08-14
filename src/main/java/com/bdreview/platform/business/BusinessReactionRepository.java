package com.bdreview.platform.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessReactionRepository extends JpaRepository<BusinessReaction, UUID> {

    boolean existsByBusinessIdAndUserIdAndReactionType(UUID businessId, UUID userId, BusinessReactionType reactionType);

    void deleteByBusinessIdAndUserIdAndReactionType(UUID businessId, UUID userId, BusinessReactionType reactionType);
}
