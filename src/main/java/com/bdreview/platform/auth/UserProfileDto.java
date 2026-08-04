package com.bdreview.platform.auth;

import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String phoneNumber,
        UserRole role,
        String name,
        String profilePhotoUrl,
        String preferredLanguage) {
}
