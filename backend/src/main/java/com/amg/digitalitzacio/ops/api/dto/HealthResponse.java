package com.amg.digitalitzacio.ops.api.dto;

import java.time.Instant;

public record HealthResponse(String status, String version, Instant timestamp, String uptime) {}
