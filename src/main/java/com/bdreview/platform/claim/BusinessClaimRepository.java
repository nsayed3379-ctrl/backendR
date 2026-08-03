package com.bdreview.platform.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessClaimRepository extends JpaRepository<BusinessClaim, UUID> {

    List<BusinessClaim> findByBusinessId(UUID businessId);

    List<BusinessClaim> findByClaimantUserId(UUID claimantUserId);

    Page<BusinessClaim> findByStatus(ClaimStatus status, Pageable pageable);
}
