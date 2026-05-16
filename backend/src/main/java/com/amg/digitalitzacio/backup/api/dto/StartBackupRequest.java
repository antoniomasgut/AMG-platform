package com.amg.digitalitzacio.backup.api.dto;

import java.util.UUID;

public record StartBackupRequest(
        String type,
        UUID tenantId
) {}
