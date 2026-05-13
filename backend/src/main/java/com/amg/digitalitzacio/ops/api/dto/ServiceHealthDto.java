package com.amg.digitalitzacio.ops.api.dto;

import java.time.Instant;

public record ServiceHealthDto(String name, String status, Long responseTimeMs, Instant lastCheck) {}
