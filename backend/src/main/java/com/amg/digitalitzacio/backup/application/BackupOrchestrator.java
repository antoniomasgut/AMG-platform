package com.amg.digitalitzacio.backup.application;

import com.amg.digitalitzacio.backup.api.dto.*;
import com.amg.digitalitzacio.backup.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupOrchestrator implements BackupService {

    private static final int RETENTION_DAYS = 30;

    private final BackupTaskRepository backupTaskRepository;
    private final BackupExportLogRepository backupExportLogRepository;
    private final RestoreTaskRepository restoreTaskRepository;
    private final ObjectMapper objectMapper;
    private final GcsClient gcsClient;

    @Value("${app.backup.gcs.bucket-name}")
    private String bucketName;

    @Override
    @Transactional
    public BackupTaskResponse startBackup(StartBackupRequest request, UUID requestedBy) {
        var type = BackupTaskType.valueOf(request.type().toUpperCase());

        // Determina l'abast i el tenantId en funció del tipus
        var scope = (type == BackupTaskType.MANUAL_TENANT) ? BackupScope.SINGLE_TENANT : BackupScope.ALL_TENANTS;
        var tenantId = (type == BackupTaskType.MANUAL_TENANT) ? request.tenantId() : null;

        if (type == BackupTaskType.MANUAL_TENANT && tenantId == null) {
            throw new ResourceNotFoundException("tenantId is required for MANUAL_TENANT backup type");
        }

        var now = Instant.now();
        var task = BackupTask.builder()
                .type(type)
                .status(BackupTaskStatus.IN_PROGRESS)
                .scope(scope)
                .tenantId(tenantId)
                .requestedBy(requestedBy)
                .totalTenants(scope == BackupScope.ALL_TENANTS ? 1 : 1)
                .completedTenants(0)
                .failedTenants(0)
                .startedAt(now)
                .retentionUntil(now.plus(Duration.ofDays(RETENTION_DAYS)))
                .build();

        task = backupTaskRepository.save(task);

        // Exporta un manifest JSON a GCS
        var key = task.getId() + "/backup-" + now.toString().substring(0, 10) + ".manifest.json";
        var manifest = ("{\"taskId\":\"" + task.getId() + "\",\"type\":\"" + type
                + "\",\"scope\":\"" + task.getScope() + "\",\"startedAt\":\"" + now + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var archivePath = gcsClient.upload(bucketName, key, manifest, "application/json");

        task.setCompletedTenants(task.getTotalTenants());
        task.setStatus(BackupTaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setArchivePath(archivePath);
        task.setArchiveSize((long) manifest.length);

        task = backupTaskRepository.save(task);
        log.info("Backup task {} completat: type={}, scope={}", task.getId(), task.getType(), task.getScope());

        return toResponse(task);
    }

    @Override
    public Page<BackupTask> listBackups(int page, int size) {
        return backupTaskRepository.findAllByOrderByStartedAtDesc(PageRequest.of(page, size));
    }

    @Override
    public BackupTaskResponse getBackup(UUID id) {
        var task = backupTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BackupTask not found: " + id));
        return toResponse(task);
    }

    @Override
    public List<BackupExportLogResponse> getBackupLogs(UUID taskId) {
        // Verifica que la tasca existeix
        backupTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("BackupTask not found: " + taskId));

        return backupExportLogRepository.findByTaskIdOrderByStartedAt(taskId)
                .stream()
                .map(this::toLogResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteBackup(UUID id) {
        var task = backupTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BackupTask not found: " + id));

        // Elimina l'arxiu de GCS si existeix
        var archivePath = task.getArchivePath();
        if (archivePath != null && archivePath.startsWith("gs://")) {
            var withoutPrefix = archivePath.substring(5); // "bucket/key"
            var slashIdx = withoutPrefix.indexOf('/');
            if (slashIdx > 0) {
                var gcsBucket = withoutPrefix.substring(0, slashIdx);
                var gcsKey = withoutPrefix.substring(slashIdx + 1);
                gcsClient.delete(gcsBucket, gcsKey);
            }
        }

        backupExportLogRepository.deleteAll(
                backupExportLogRepository.findByTaskIdOrderByStartedAt(id));
        backupTaskRepository.delete(task);
        log.info("BackupTask {} eliminat", id);
    }

    @Override
    public BackupDashboardResponse getDashboard() {
        var allTasks = backupTaskRepository.findAll();
        long total = allTasks.size();

        var lastTask = backupTaskRepository.findTopByOrderByStartedAtDesc();
        Instant lastBackup = lastTask.map(BackupTask::getStartedAt).orElse(null);
        String lastBackupStatus = lastTask.map(t -> t.getStatus().name()).orElse(null);

        long scheduledCount = allTasks.stream()
                .filter(t -> t.getType() == BackupTaskType.SCHEDULED).count();
        long manualFullCount = allTasks.stream()
                .filter(t -> t.getType() == BackupTaskType.MANUAL_FULL).count();
        long manualTenantCount = allTasks.stream()
                .filter(t -> t.getType() == BackupTaskType.MANUAL_TENANT).count();

        long storageUsed = allTasks.stream()
                .filter(t -> t.getArchiveSize() != null)
                .mapToLong(BackupTask::getArchiveSize)
                .sum();

        // Proper Sunday at 04:00 UTC
        var nextSunday = calculateNextSundayAt4AmUtc();

        return new BackupDashboardResponse(
                total,
                lastBackup,
                lastBackupStatus,
                scheduledCount,
                manualFullCount,
                manualTenantCount,
                storageUsed,
                nextSunday,
                RETENTION_DAYS
        );
    }

    @Override
    @Transactional
    public RestoreResponse startRestore(RestoreRequest request, UUID requestedBy) {
        // Serialitza les seccions com a JSON
        String sectionsJson = serializeSections(request.sections());

        var restore = RestoreTask.builder()
                .tenantId(request.tenantId())
                .backupTaskId(request.backupTaskId())
                .status(RestoreStatus.IN_PROGRESS)
                .sections(sectionsJson)
                .requestedBy(requestedBy)
                .build();

        restore = restoreTaskRepository.save(restore);
        log.info("RestoreTask {} iniciat per al tenant {} (backupTaskId={})",
                restore.getId(), restore.getTenantId(), request.backupTaskId());

        try {
            // Recupera el manifest de GCS per al backupTaskId
            if (request.backupTaskId() != null) {
                var backupTask = backupTaskRepository.findById(request.backupTaskId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "BackupTask not found: " + request.backupTaskId()));

                var archivePath = backupTask.getArchivePath();
                if (archivePath != null && archivePath.startsWith("gs://")) {
                    var withoutPrefix = archivePath.substring(5);
                    var slashIdx = withoutPrefix.indexOf('/');
                    if (slashIdx > 0) {
                        var gcsBucket = withoutPrefix.substring(0, slashIdx);
                        var gcsKey = withoutPrefix.substring(slashIdx + 1);

                        // Descarrega el manifest de GCS
                        if (gcsClient.exists(gcsBucket, gcsKey)) {
                            var manifestBytes = gcsClient.download(gcsBucket, gcsKey);
                            log.info("Manifest descarregat de gs://{}/{} ({} bytes)",
                                    gcsBucket, gcsKey, manifestBytes.length);
                        }
                    }
                }
            }

            restore.setStatus(RestoreStatus.COMPLETED);
            restore.setCompletedAt(Instant.now());
            log.info("RestoreTask {} completat per al tenant {}", restore.getId(), restore.getTenantId());

        } catch (Exception e) {
            restore.setStatus(RestoreStatus.FAILED);
            restore.setErrorMessage(e.getMessage());
            log.error("RestoreTask {} fallat per al tenant {}: {}",
                    restore.getId(), restore.getTenantId(), e.getMessage());
        }

        restore = restoreTaskRepository.save(restore);
        return toRestoreResponse(restore);
    }

    @Override
    public RestoreResponse getRestore(UUID id) {
        var restore = restoreTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestoreTask not found: " + id));
        return toRestoreResponse(restore);
    }

    @Override
    @Transactional
    public BackupTaskResponse exportTenant(UUID tenantId, UUID requestedBy) {
        var now = Instant.now();
        var key = "tenant-" + tenantId + "/export-" + now.toString().substring(0, 10) + ".manifest.json";
        var manifest = ("{\"tenantId\":\"" + tenantId + "\",\"exportedAt\":\"" + now + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var archivePath = gcsClient.upload(bucketName, key, manifest, "application/json");

        var task = BackupTask.builder()
                .type(BackupTaskType.MANUAL_TENANT)
                .status(BackupTaskStatus.COMPLETED)
                .scope(BackupScope.SINGLE_TENANT)
                .tenantId(tenantId)
                .requestedBy(requestedBy)
                .totalTenants(1)
                .completedTenants(1)
                .failedTenants(0)
                .startedAt(now)
                .completedAt(now)
                .retentionUntil(now.plus(Duration.ofDays(RETENTION_DAYS)))
                .archivePath(archivePath)
                .archiveSize((long) manifest.length)
                .build();

        task = backupTaskRepository.save(task);
        log.info("Export tenant {} creat: taskId={}", tenantId, task.getId());

        return toResponse(task);
    }

    // ── Helpers de mapatge ──────────────────────────────────────────────────

    private BackupTaskResponse toResponse(BackupTask task) {
        return new BackupTaskResponse(
                task.getId(),
                task.getType().name(),
                task.getStatus().name(),
                task.getScope().name(),
                task.getTenantId(),
                task.getRequestedBy(),
                task.getTotalTenants(),
                task.getCompletedTenants(),
                task.getFailedTenants(),
                task.getArchiveSize(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getRetentionUntil()
        );
    }

    private BackupExportLogResponse toLogResponse(BackupExportLog log) {
        return new BackupExportLogResponse(
                log.getId(),
                log.getTaskId(),
                log.getTenantId(),
                log.getTenantName(),
                log.getStatus().name(),
                log.getFileSize(),
                log.getSectionsCount(),
                log.getErrorMessage(),
                log.getStartedAt(),
                log.getCompletedAt()
        );
    }

    private RestoreResponse toRestoreResponse(RestoreTask restore) {
        List<String> sections = deserializeSections(restore.getSections());
        return new RestoreResponse(
                restore.getId(),
                restore.getTenantId(),
                restore.getStatus().name(),
                sections,
                restore.getCreatedAt()
        );
    }

    private String serializeSections(List<String> sections) {
        if (sections == null || sections.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JsonProcessingException e) {
            log.warn("Error serialitzant seccions, usant representació per defecte", e);
            return "[]";
        }
    }

    private List<String> deserializeSections(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Error deserialitzant seccions: {}", json, e);
            return List.of();
        }
    }

    private Instant calculateNextSundayAt4AmUtc() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var dayOfWeek = now.getDayOfWeek().getValue(); // 1=Monday..7=Sunday
        int daysUntilSunday = (7 - dayOfWeek) % 7;
        if (daysUntilSunday == 0 && now.getHour() >= 4) {
            daysUntilSunday = 7; // ja ha passat les 4h d'avui (diumenge), anar al vinent
        }
        return now.toLocalDate()
                .plusDays(daysUntilSunday)
                .atTime(4, 0)
                .toInstant(ZoneOffset.UTC);
    }
}
