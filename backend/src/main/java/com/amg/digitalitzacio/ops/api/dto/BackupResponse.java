package com.amg.digitalitzacio.ops.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BackupResponse(UUID id, String type, String status, Instant startedAt) {}
