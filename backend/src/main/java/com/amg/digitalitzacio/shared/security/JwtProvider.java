package com.amg.digitalitzacio.shared.security;

import com.amg.digitalitzacio.shared.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(JwtProperties props) {
        if (props.secret() == null || props.secret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured in environment");
        }
        this.secretKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = props.accessTokenExpiration();
        this.refreshExpiration = props.refreshTokenExpiration();
    }

    public String generateAccessToken(UUID userId, String email, Role role, UUID tenantId) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("tenantId", tenantId != null ? tenantId.toString() : null)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpiration)))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
