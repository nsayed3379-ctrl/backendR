package com.bdreview.platform.auth;

public record TokenPairDto(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {
}
