package com.amg.digitalitzacio.backup.api;

import com.amg.digitalitzacio.backup.api.dto.*;
import com.amg.digitalitzacio.backup.application.BackupService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/api/v1/ops/backups")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BackupTaskResponse startBackup(
            @RequestBody StartBackupRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return backupService.startBackup(request, principal.id());
    }

    @GetMapping("/api/v1/ops/backups")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BackupListResponse listBackups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var backupPage = backupService.listBackups(page, size);
        var content = backupPage.getContent().stream()
                .map(t -> backupService.getBackup(t.getId()))
                .toList();
        return new BackupListResponse(
                content,
                backupPage.getTotalElements(),
                backupPage.getTotalPages(),
                page,
                size
        );
    }

    @GetMapping("/api/v1/ops/backups/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BackupDashboardResponse getDashboard() {
        return backupService.getDashboard();
    }

    @GetMapping("/api/v1/ops/backups/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BackupTaskResponse getBackup(@PathVariable UUID id) {
        return backupService.getBackup(id);
    }

    @GetMapping("/api/v1/ops/backups/{id}/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<BackupExportLogResponse> getBackupLogs(@PathVariable UUID id) {
        return backupService.getBackupLogs(id);
    }

    @DeleteMapping("/api/v1/ops/backups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteBackup(@PathVariable UUID id) {
        backupService.deleteBackup(id);
    }

    @PostMapping("/api/v1/ops/backups/restore")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RestoreResponse startRestore(
            @RequestBody RestoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return backupService.startRestore(request, principal.id());
    }

    @GetMapping("/api/v1/ops/backups/restore/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RestoreResponse getRestore(@PathVariable UUID id) {
        return backupService.getRestore(id);
    }

    @PostMapping("/api/v1/ops/backups/export/{tenantId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BackupTaskResponse exportTenant(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return backupService.exportTenant(tenantId, principal.id());
    }
}
