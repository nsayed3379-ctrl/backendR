package com.bdreview.platform.common;

/**
 * Shared pagination bounds (spec §3): search results must always be paginated
 * (20-30 per page) — never load everything at once. Every DAO-facing listing
 * query in this codebase should clamp against these constants.
 */
public final class PageRequestDefaults {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 30;

    private PageRequestDefaults() {
    }

    public static int clamp(int requestedSize) {
        if (requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }
}
