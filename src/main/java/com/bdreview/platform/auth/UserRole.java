package com.bdreview.platform.auth;

/**
 * One account, one role (finalized decision) — a phone number cannot hold both
 * CONSUMER and BUSINESS_OWNER simultaneously (spec §5, public signup flow).
 * ADMIN is a separate, internally-provisioned account type for moderation
 * staff (spec §12) — never created through the public OTP registration flow.
 */
public enum UserRole {
    CONSUMER,
    BUSINESS_OWNER,
    ADMIN
}
