package com.bdreview.platform.admin.support;

import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Small helpers shared across the admin controllers. Kept separate from
 * {@code common.CurrentUser} (which the existing moderation services already
 * rely on and which continues to work unmodified for admin-panel requests,
 * since {@link com.bdreview.platform.admin.config.AdminAuthenticationProvider}
 * sets the authenticated principal name to the admin's UUID) — this class is
 * just for admin controllers that want the id directly from the injected
 * {@link Authentication} rather than via the SecurityContextHolder-reading
 * static helper.
 */
public final class AdminSupport {

    public static final int PAGE_SIZE = 20;

    private AdminSupport() {
    }

    public static UUID currentAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    public static int pageOrDefault(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }
}
