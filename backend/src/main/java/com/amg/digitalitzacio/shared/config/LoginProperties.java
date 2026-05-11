package com.amg.digitalitzacio.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "login")
public record LoginProperties(
        int maxAttempts,
        int lockDurationMinutes
) {}
