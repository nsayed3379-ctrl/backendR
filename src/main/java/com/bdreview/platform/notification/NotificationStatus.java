package com.bdreview.platform.notification;

/**
 * PENDING -> SENT/FAILED tracks delivery (meaningful mainly for SMS; IN_APP
 * has no external delivery step, so it moves straight to SENT). READ is a
 * separate axis — whether the *recipient* opened it — but reuses this same
 * field rather than a second column, matching the requested schema.
 */
public enum NotificationStatus {
    PENDING, SENT, FAILED, READ
}
