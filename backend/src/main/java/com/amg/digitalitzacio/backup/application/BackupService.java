package com.amg.digitalitzacio.backup.application;

import com.amg.digitalitzacio.backup.api.dto.*;
import com.amg.digitalitzacio.backup.domain.BackupTask;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface BackupService {

    BackupTaskResponse startBackup(StartBackupRequest request, UUID requestedBy);

    Page<BackupTask> listBackups(int page, int size);

    BackupTaskResponse getBackup(UUID id);

    List<BackupExportLogResponse> getBackupLogs(UUID taskId);

    void deleteBackup(UUID id);

    BackupDashboardResponse getDashboard();

    RestoreResponse startRestore(RestoreRequest request, UUID requestedBy);

    RestoreResponse getRestore(UUID id);

    BackupTaskResponse exportTenant(UUID tenantId, UUID requestedBy);
}
