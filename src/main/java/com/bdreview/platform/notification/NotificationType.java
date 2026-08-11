package com.bdreview.platform.notification;

/** Kept deliberately small and extensible — add new values (+ a migration widening the CHECK constraint) as needed. */
public enum NotificationType {
    REPORT_SUBMITTED,
    REPORT_ACTION_TAKEN,
    REPORT_DISMISSED,
    CONTENT_HIDDEN,
    LISTING_FLAGGED,
    FLAG_REVIEW_REQUESTED
}
