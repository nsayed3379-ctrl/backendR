package com.bdreview.platform.report;

import jakarta.validation.constraints.NotNull;

/** {@code outcome} must be ACTION_TAKEN, DISMISSED, or DUPLICATE — never PENDING (enforced in ReportService). */
public record ResolveReportRequest(@NotNull ReportStatus outcome, String resolutionNote) {
}
