package com.bdreview.platform.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    /** Rate-limit backing query: caps how many reports one user can file in a short window (spec §11). */
    long countByReporterUserIdAndCreatedAtAfter(UUID reporterUserId, Instant since);

    /** Rate-limit backing query: caps repeat reports from the same reporter against the same target. */
    long countByReporterUserIdAndTargetTypeAndTargetIdAndCreatedAtAfter(
            UUID reporterUserId, ReportTargetType targetType, UUID targetId, Instant since);

    /** Auto-triage + admin-queue context: total reports (any reporter, any status) against one target. */
    long countByTargetTypeAndTargetId(ReportTargetType targetType, UUID targetId);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    /** Reporter credibility (admin queue): total reports this user has ever filed. */
    long countByReporterUserId(UUID reporterUserId);

    /** Reporter credibility (admin queue): how many of those were upheld (ACTION_TAKEN). */
    long countByReporterUserIdAndStatus(UUID reporterUserId, ReportStatus status);

    /** Backs the "R-<n>" reference code — one fresh value per report, assigned in ReportService#create. */
    @Query(value = "SELECT nextval('report_reference_seq')", nativeQuery = true)
    long nextReferenceSeqValue();
}
