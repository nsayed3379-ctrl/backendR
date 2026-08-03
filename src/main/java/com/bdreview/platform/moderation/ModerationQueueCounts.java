package com.bdreview.platform.moderation;

public record ModerationQueueCounts(long pendingReports, long pendingNidVerifications, long flaggedReviews) {
}
