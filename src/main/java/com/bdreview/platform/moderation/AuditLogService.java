package com.bdreview.platform.moderation;

import org.springframework.stereotype.Service;

import java.util.UUID;

/** Thin wrapper so every admin resolution action across packages logs consistently (spec §12). */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String entityType, UUID entityId, String action, UUID performedByAdmin, String notes) {
        auditLogRepository.save(AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedByAdmin(performedByAdmin)
                .notes(notes)
                .build());
    }
}
