package com.bdreview.platform.report;

import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.RateLimitExceededException;
import com.bdreview.platform.common.ResourceNotFoundException;
import com.bdreview.platform.moderation.AuditLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Spec §11: fixed enum reason (never free text), rate-limited to prevent report-spam abuse. */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final AuditLogService auditLogService;
    private final long maxReportsPerHour;

    public ReportService(ReportRepository reportRepository,
                          AuditLogService auditLogService,
                          @Value("${app.report.max-per-hour:10}") long maxReportsPerHour) {
        this.reportRepository = reportRepository;
        this.auditLogService = auditLogService;
        this.maxReportsPerHour = maxReportsPerHour;
    }

    @Transactional
    public Report create(UUID reporterUserId, CreateReportRequest request) {
        long recent = reportRepository.countByReporterUserIdAndCreatedAtAfter(
                reporterUserId, Instant.now().minus(1, ChronoUnit.HOURS));
        if (recent >= maxReportsPerHour) {
            throw new RateLimitExceededException("Too many reports filed recently — please try again later.");
        }

        return reportRepository.save(Report.builder()
                .reporterUserId(reporterUserId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .status(ReportStatus.PENDING)
                .build());
    }

    public Page<Report> queue(Pageable pageable) {
        return reportRepository.findByStatus(ReportStatus.PENDING, pageable);
    }

    @Transactional
    public void resolve(UUID reportId, String notes) {
        CurrentUser.requireRole("ADMIN");
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(ReportStatus.RESOLVED);
        reportRepository.save(report);
        auditLogService.log("REPORT", reportId, "RESOLVED", CurrentUser.id(), notes);
    }
}
