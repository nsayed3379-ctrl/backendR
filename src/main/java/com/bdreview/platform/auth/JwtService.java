package com.bdreview.platform.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Short-lived access tokens (15-30 min) signed with HS256 (spec §5). Refresh
 * tokens are NOT JWTs — they're opaque random strings stored hashed in
 * {@link RefreshToken}, per the spec's "stored hashed, rotated on every use".
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenTtlMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        this.key = Keys.hmacShaKeyFor(pad(secret).getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    // HS256 needs >= 256-bit key; pad short dev secrets so local runs don't blow up.
    private static String pad(String secret) {
        StringBuilder sb = new StringBuilder(secret);
        while (sb.length() < 32) {
            sb.append(secret);
        }
        return sb.substring(0, Math.max(32, secret.length()));
    }

    public String generateAccessToken(UUID userId, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
