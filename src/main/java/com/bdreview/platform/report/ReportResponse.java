package com.bdreview.platform.report;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id, UUID reporterUserId, String reporterName, ReportTargetType targetType, UUID targetId,
        ReportReason reason, ReportStatus status, String referenceCode, String resolutionNote,
        Instant reporterNotifiedAt, Instant targetOwnerNotifiedAt, Priority priority, Instant dueAt,
        boolean isOverdue, Instant createdAt
) {
    public static ReportResponse from(Report r, String reporterName) {
        return new ReportResponse(r.getId(), r.getReporterUserId(), reporterName, r.getTargetType(), r.getTargetId(),
                r.getReason(), r.getStatus(), r.getReferenceCode(), r.getResolutionNote(),
                r.getReporterNotifiedAt(), r.getTargetOwnerNotifiedAt(), r.getPriority(), r.getDueAt(),
                r.isOverdue(), r.getCreatedAt());
    }
}
