package com.bdreview.platform.claim;

import com.bdreview.platform.business.Business;
import com.bdreview.platform.business.BusinessRepository;
import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.ResourceNotFoundException;
import com.bdreview.platform.moderation.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Spec §2: real owner claims an admin-seeded or already-listed business via phone/NID verification. */
@Service
public class BusinessClaimService {

    private final BusinessClaimRepository claimRepository;
    private final BusinessRepository businessRepository;
    private final AuditLogService auditLogService;

    public BusinessClaimService(BusinessClaimRepository claimRepository,
                                 BusinessRepository businessRepository,
                                 AuditLogService auditLogService) {
        this.claimRepository = claimRepository;
        this.businessRepository = businessRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public BusinessClaim fileClaim(UUID claimantUserId, FileClaimRequest request) {
        businessRepository.findById(request.businessId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return claimRepository.save(BusinessClaim.builder()
                .businessId(request.businessId())
                .claimantUserId(claimantUserId)
                .verificationMethod(request.verificationMethod())
                .status(ClaimStatus.PENDING)
                .build());
    }

    /** §2 duplicate check exposed for the create-listing flow — prompts "claim existing or proceed new". */
    public List<Business> findPotentialDuplicates(UUID categoryId, UUID areaId, String name) {
        return businessRepository.findPotentialDuplicates(categoryId, areaId, name);
    }

    public List<BusinessClaim> myClaims(UUID claimantUserId) {
        return claimRepository.findByClaimantUserId(claimantUserId);
    }

    public Page<BusinessClaim> queue(Pageable pageable) {
        return claimRepository.findByStatus(ClaimStatus.PENDING, pageable);
    }

    @Transactional
    public BusinessClaim resolve(UUID claimId, ResolveClaimRequest request) {
        CurrentUser.requireRole("ADMIN");
        BusinessClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        claim.setStatus(request.approve() ? ClaimStatus.APPROVED : ClaimStatus.REJECTED);
        claim.setResolvedAt(Instant.now());
        claimRepository.save(claim);

        if (request.approve()) {
            businessRepository.updateOwner(claim.getBusinessId(), claim.getClaimantUserId());
        }

        auditLogService.log("BUSINESS_CLAIM", claimId,
                request.approve() ? "APPROVED" : "REJECTED", CurrentUser.id(), request.notes());

        return claim;
    }
}
