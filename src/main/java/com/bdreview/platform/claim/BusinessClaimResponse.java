package com.bdreview.platform.claim;

import java.time.Instant;
import java.util.UUID;

public record BusinessClaimResponse(
        UUID id, UUID businessId, UUID claimantUserId, String claimantName,
        VerificationMethod verificationMethod, ClaimStatus status, Instant createdAt, Instant resolvedAt
) {
    public static BusinessClaimResponse from(BusinessClaim c, String claimantName) {
        return new BusinessClaimResponse(c.getId(), c.getBusinessId(), c.getClaimantUserId(), claimantName,
                c.getVerificationMethod(), c.getStatus(), c.getCreatedAt(), c.getResolvedAt());
    }
}
