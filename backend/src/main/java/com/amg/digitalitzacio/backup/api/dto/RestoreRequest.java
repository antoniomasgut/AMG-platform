package com.amg.digitalitzacio.backup.api.dto;

import java.util.List;
import java.util.UUID;

public record RestoreRequest(
        UUID tenantId,
        UUID backupTaskId,
        List<String> sections
) {}
