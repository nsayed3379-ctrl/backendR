package com.bdreview.platform.report;

/**
 * PENDING is the only non-terminal value; the other three are resolution
 * outcomes an admin chooses on {@code POST /reports/{id}/resolve} (spec:
 * production-grade report workflow). There is deliberately no separate
 * "outcome" type — the status *is* the outcome once a report is resolved.
 */
public enum ReportStatus {
    PENDING, ACTION_TAKEN, DISMISSED, DUPLICATE;

    public boolean isResolutionOutcome() {
        return this != PENDING;
    }
}
