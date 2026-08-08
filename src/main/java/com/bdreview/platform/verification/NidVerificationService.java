package com.bdreview.platform.verification;

import com.bdreview.platform.business.Business;
import com.bdreview.platform.business.BusinessRepository;
import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.ForbiddenException;
import com.bdreview.platform.common.ResourceNotFoundException;
import com.bdreview.platform.moderation.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Spec §8. This service never returns a raw/public URL for an NID image —
 * only the opaque `encryptedImageRef` pointer is stored/returned; issuing a
 * short-lived admin-only signed URL for actually viewing it is a separate,
 * infra-specific concern (object storage client) outside this DAO-facing scope.
 */
@Service
public class NidVerificationService {

    /** Data-minimization: raw image purge window after resolution (spec §8). */
    private static final int PURGE_AFTER_DAYS = 30;

    private final NidVerificationRepository repository;
    private final BusinessRepository businessRepository;
    private final AuditLogService auditLogService;

    public NidVerificationService(NidVerificationRepository repository,
                                   BusinessRepository businessRepository,
                                   AuditLogService auditLogService) {
        this.repository = repository;
        this.businessRepository = businessRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public NidVerification submit(UUID ownerUserId, SubmitNidRequest request) {
        Business business = businessRepository.findById(request.businessId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        if (!business.getOwnerUserId().equals(ownerUserId)) {
            throw new ForbiddenException("You do not own this business listing");
        }

        return repository.save(NidVerification.builder()
                .businessId(business.getId())
                .ownerUserId(ownerUserId)
                .encryptedImageRef(request.encryptedImageRef())
                .status(NidVerificationStatus.PENDING)
                .build());
    }

    public List<NidVerification> history(UUID businessId) {
        return repository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    public Page<NidVerification> queue(Pageable pageable) {
        return repository.findByStatus(NidVerificationStatus.PENDING, pageable);
    }

    @Transactional
    public void resolve(UUID id, ResolveNidRequest request) {
        CurrentUser.requireRole("ADMIN");
        NidVerification nid = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NID verification not found"));

        NidVerificationStatus status = request.approve() ? NidVerificationStatus.APPROVED : NidVerificationStatus.REJECTED;
        Instant now = Instant.now();
        Instant purgeAt = now.plus(PURGE_AFTER_DAYS, ChronoUnit.DAYS);

        repository.resolve(id, status, CurrentUser.id(), now, purgeAt);

        if (request.approve()) {
            businessRepository.markVerified(nid.getBusinessId());
        }
        // On rejection, the owner can resubmit — a fresh row via submit(), so nothing to reset here.

        auditLogService.log("NID_VERIFICATION", id,
                request.approve() ? "APPROVED" : "REJECTED", CurrentUser.id(), request.notes());
    }
}
