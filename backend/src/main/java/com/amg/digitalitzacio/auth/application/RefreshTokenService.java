package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.shared.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redis;
    private final JwtProperties jwtProperties;

    public void save(String tokenId, UUID userId) {
        var key = KEY_PREFIX + tokenId;
        redis.opsForValue().set(key, userId.toString(),
                jwtProperties.refreshTokenExpiration(), TimeUnit.MILLISECONDS);
    }

    public boolean isValid(String tokenId, UUID userId) {
        var key = KEY_PREFIX + tokenId;
        var stored = redis.opsForValue().get(key);
        return stored != null && stored.equals(userId.toString());
    }

    public void delete(String tokenId) {
        redis.delete(KEY_PREFIX + tokenId);
    }

    public String getUserId(String tokenId) {
        return redis.opsForValue().get(KEY_PREFIX + tokenId);
    }

    public void deleteAllForUser(UUID userId) {
        var pattern = KEY_PREFIX + "*";
        try (var connection = redis.getConnectionFactory().getConnection()) {
            var keys = connection.keys((KEY_PREFIX + "*").getBytes());
            for (var keyBytes : keys) {
                var key = new String(keyBytes);
                var stored = redis.opsForValue().get(key);
                if (userId.toString().equals(stored)) {
                    redis.delete(key);
                }
            }
        }
    }

    public static String hashToken(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(token.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
