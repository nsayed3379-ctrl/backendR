package com.bdreview.platform.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    /** Rate-limit backing query: caps how many reports one user can file in a short window (spec §11). */
    long countByReporterUserIdAndCreatedAtAfter(UUID reporterUserId, Instant since);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
}
