package com.bdreview.platform.review;

/**
 * Derived from the fake-review suspicion score (spec §14):
 *  0-30  -> RECOMMENDED       (shown normally, counts fully toward aggregates)
 *  31-70 -> NOT_RECOMMENDED   ("not recommended" section, excluded from aggregates, queued for admin)
 *  71-100-> HIDDEN            (auto-hidden from all public views, excluded from aggregates)
 */
public enum VisibilityStatus {
    RECOMMENDED,
    NOT_RECOMMENDED,
    HIDDEN
}
