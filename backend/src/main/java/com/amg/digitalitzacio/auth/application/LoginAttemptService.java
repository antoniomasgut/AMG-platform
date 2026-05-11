package com.amg.digitalitzacio.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final String EMAIL_PREFIX = "login_attempt:email:";
    private static final String IP_PREFIX = "login_attempt:ip:";

    private final StringRedisTemplate redis;
    private final int maxPerEmail;
    private final int maxPerIp;
    private final long windowMinutes;

    public LoginAttemptService(
            StringRedisTemplate redis,
            @Value("${rate-limiting.max-attempts-per-email:5}") int maxPerEmail,
            @Value("${rate-limiting.max-attempts-per-ip:20}") int maxPerIp,
            @Value("${rate-limiting.window-minutes:15}") long windowMinutes) {
        this.redis = redis;
        this.maxPerEmail = maxPerEmail;
        this.maxPerIp = maxPerIp;
        this.windowMinutes = windowMinutes;
    }

    public boolean isBlocked(String email, String ip) {
        var emailCount = redis.opsForValue().get(EMAIL_PREFIX + email);
        if (emailCount != null && Integer.parseInt(emailCount) >= maxPerEmail) {
            return true;
        }
        var ipCount = redis.opsForValue().get(IP_PREFIX + ip);
        return ipCount != null && Integer.parseInt(ipCount) >= maxPerIp;
    }

    public void registerAttempt(String email, String ip) {
        redis.opsForValue().increment(EMAIL_PREFIX + email);
        redis.expire(EMAIL_PREFIX + email, windowMinutes, TimeUnit.MINUTES);

        redis.opsForValue().increment(IP_PREFIX + ip);
        redis.expire(IP_PREFIX + ip, windowMinutes, TimeUnit.MINUTES);
    }

    public void resetForEmail(String email) {
        redis.delete(EMAIL_PREFIX + email);
    }
}
