package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.shared.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh_token:";
    private static final String USED_PREFIX = "refresh_token_used:";
    private static final String VALUE_SEPARATOR = ":";

    private final StringRedisTemplate redis;
    private final JwtProperties jwtProperties;

    /**
     * Store a refresh token in Redis.
     * Value format: "{userId}:{sha256(token)}"
     */
    public void save(String tokenId, UUID userId, String tokenHash) {
        var key = KEY_PREFIX + tokenId;
        var value = userId.toString() + VALUE_SEPARATOR + tokenHash;
        redis.opsForValue().set(key, value,
                jwtProperties.refreshTokenExpiration(), TimeUnit.MILLISECONDS);
    }

    /**
     * Get the raw stored value ("{userId}:{tokenHash}") for a tokenId.
     * Returns null if the token doesn't exist or has expired.
     */
    public String getStoredValue(String tokenId) {
        return redis.opsForValue().get(KEY_PREFIX + tokenId);
    }

    /**
     * Extract userId from a stored value ("{userId}:{tokenHash}").
     */
    public UUID getUserIdFromValue(String storedValue) {
        var parts = storedValue.split(VALUE_SEPARATOR, 2);
        return UUID.fromString(parts[0]);
    }

    /**
     * Extract token hash from a stored value ("{userId}:{tokenHash}").
     */
    public String getTokenHash(String storedValue) {
        var parts = storedValue.split(VALUE_SEPARATOR, 2);
        return parts.length == 2 ? parts[1] : "";
    }

    /**
     * Get userId for a tokenId (simplified lookup, for backwards compatibility).
     * Returns null if token doesn't exist.
     */
    public String getUserId(String tokenId) {
        var stored = getStoredValue(tokenId);
        if (stored == null) return null;
        try {
            return getUserIdFromValue(stored).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValid(String tokenId, UUID userId) {
        var stored = getStoredValue(tokenId);
        if (stored == null) return false;
        try {
            return userId.equals(getUserIdFromValue(stored));
        } catch (Exception e) {
            return false;
        }
    }

    public void delete(String tokenId) {
        redis.delete(KEY_PREFIX + tokenId);
    }

    /**
     * Mark a token as used. Stored separately so we can detect reuse
     * even after the original key has been deleted.
     */
    public void markUsed(String tokenId) {
        var key = USED_PREFIX + tokenId;
        redis.opsForValue().set(key, "1",
                jwtProperties.refreshTokenExpiration(), TimeUnit.MILLISECONDS);
    }

    /**
     * Check if a token was previously used.
     */
    public boolean isUsed(String tokenId) {
        return Boolean.TRUE.equals(redis.hasKey(USED_PREFIX + tokenId));
    }

    public void deleteAllForUser(UUID userId) {
        String userIdStr = userId.toString();
        var scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(KEY_PREFIX + "*")
                .count(100)
                .build();
        try (var cursor = redis.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                var stored = redis.opsForValue().get(key);
                if (stored != null) {
                    var parts = stored.split(VALUE_SEPARATOR, 2);
                    if (parts.length > 0 && userIdStr.equals(parts[0])) {
                        redis.delete(key);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error during Redis scan for user {}: {}", userId, e.getMessage());
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
