package com.bdreview.platform.report;

/** Admin-facing trust signal for a reporter, derived from their own report history — no new table. */
public record ReporterCredibility(long totalReports, long actionTakenReports) {

    public String accuracyLabel() {
        return actionTakenReports + "/" + totalReports;
    }
}
