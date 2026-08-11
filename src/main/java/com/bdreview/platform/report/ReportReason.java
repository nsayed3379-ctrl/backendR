package com.bdreview.platform.report;

/** Fixed enum reason — deliberately not free text (spec §11). */
public enum ReportReason {
    SPAM, FAKE, OFFENSIVE, OTHER;

    /** Human-readable form used in the generic outcome notifications sent to a target owner. */
    public String label() {
        return switch (this) {
            case SPAM -> "spam";
            case FAKE -> "fake or misleading content";
            case OFFENSIVE -> "offensive content";
            case OTHER -> "a community-guidelines violation";
        };
    }
}
