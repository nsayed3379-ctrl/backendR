package com.bdreview.platform.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async so the fake-review ML analysis (fakereview package) and the
 * summary-regeneration check (summary package) never block the request thread
 * that created/edited a review — matching the spec's "must not wait on the
 * fake-review ML analysis" requirement for the rating-aggregate fast path.
 * The report workflow's notification dispatch (report package, via
 * NotificationService) reuses the same self-injected-proxy @Async pattern so
 * SMS/in-app delivery never blocks report submission or resolution either.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
