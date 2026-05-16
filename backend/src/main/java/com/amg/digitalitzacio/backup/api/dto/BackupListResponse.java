package com.amg.digitalitzacio.backup.api.dto;

import java.util.List;

public record BackupListResponse(
        List<BackupTaskResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
