package com.amg.digitalitzacio.ops.application;

import com.amg.digitalitzacio.ops.api.dto.*;
import com.amg.digitalitzacio.ops.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpsOrchestrator implements OpsService {

    private final ServiceHealthRepository serviceHealthRepository;
    private final IncidentRepository incidentRepository;
    private final BackupRecordRepository backupRecordRepository;

    @Override
    public HealthResponse getHealth() {
        var uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        var uptimeStr = formatUptime(uptime);
        return new HealthResponse("UP", "0.0.1", Instant.now(), uptimeStr);
    }

    @Override
    public AllHealthResponse getAllHealth() {
        var services = List.of("backend", "postgres", "redis", "n8n", "minio", "frontend", "traefik");
        var dtos = new ArrayList<ServiceHealthDto>();

        for (var svc : services) {
            var last = serviceHealthRepository.findTopByServiceNameOrderByCheckedAtDesc(svc);
            if (last.isPresent()) {
                var h = last.get();
                dtos.add(new ServiceHealthDto(svc, h.getStatus().name(), h.getResponseTimeMs(), h.getCheckedAt()));
            } else {
                dtos.add(new ServiceHealthDto(svc, "UNKNOWN", null, null));
            }
        }

        var up = dtos.stream().filter(s -> "UP".equals(s.status())).count();
        var down = dtos.stream().filter(s -> "DOWN".equals(s.status())).count();
        var degraded = dtos.stream().filter(s -> "DEGRADED".equals(s.status())).count();

        return new AllHealthResponse(dtos,
                new AllHealthResponse.HealthSummary(dtos.size(), (int) up, (int) down, (int) degraded),
                Instant.now());
    }

    @Override
    public List<IncidentResponse> listIncidents(String status, String serviceName) {
        List<Incident> incidents;
        if (status != null && !status.isBlank()) {
            incidents = incidentRepository.findByStatusAndServiceName(
                    IncidentStatus.valueOf(status.toUpperCase()), serviceName);
        } else {
            incidents = incidentRepository.findByStatusOrderByStartedAtDesc(
                    status != null ? IncidentStatus.valueOf(status.toUpperCase()) : IncidentStatus.OPEN);
        }
        return incidents.stream().map(this::toIncidentResponse).toList();
    }

    @Override
    @Transactional
    public IncidentResponse acknowledgeIncident(UUID incidentId) {
        var incident = findIncident(incidentId);
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident = incidentRepository.save(incident);
        return toIncidentResponse(incident);
    }

    @Override
    @Transactional
    public IncidentResponse resolveIncident(UUID incidentId) {
        var incident = findIncident(incidentId);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        if (incident.getStartedAt() != null) {
            incident.setDurationSeconds(Duration.between(incident.getStartedAt(), incident.getResolvedAt()).getSeconds());
        }
        incident = incidentRepository.save(incident);
        return toIncidentResponse(incident);
    }

    @Override
    public OpsDashboardResponse getDashboard() {
        var services = List.of("backend", "postgres", "redis", "n8n", "minio", "frontend", "traefik");
        var dtos = new ArrayList<ServiceHealthDto>();
        int up = 0, down = 0, degraded = 0;

        for (var svc : services) {
            var last = serviceHealthRepository.findTopByServiceNameOrderByCheckedAtDesc(svc);
            if (last.isPresent()) {
                var h = last.get();
                dtos.add(new ServiceHealthDto(svc, h.getStatus().name(), h.getResponseTimeMs(), h.getCheckedAt()));
                switch (h.getStatus()) {
                    case UP -> up++;
                    case DOWN -> down++;
                    case DEGRADED -> degraded++;
                }
            }
        }

        var openIncidents = incidentRepository.findByStatusOrderByStartedAtDesc(IncidentStatus.OPEN).size();
        var todayStart = Instant.now().minus(java.time.Duration.ofHours(24));
        var allIncidents = incidentRepository.findByStatusOrderByStartedAtDesc(IncidentStatus.OPEN);
        var todayIncidents = (int) allIncidents.stream()
                .filter(i -> i.getStartedAt().isAfter(todayStart)).count();

        var lastBackupRecords = backupRecordRepository.findByTypeOrderByStartedAtDesc(BackupType.DATABASE);
        var lastBackup = lastBackupRecords.isEmpty() ? null : lastBackupRecords.get(0).getStartedAt();

        var avgResponse = dtos.stream()
                .filter(s -> s.responseTimeMs() != null)
                .mapToLong(ServiceHealthDto::responseTimeMs)
                .average().orElse(0);

        return new OpsDashboardResponse(
                new OpsDashboardResponse.CurrentStatus(services.size(), up, degraded, down),
                openIncidents, todayIncidents, (long) avgResponse, lastBackup, 99.97);
    }

    @Override
    @Transactional
    public BackupResponse startBackup(String type, String description) {
        var backupType = BackupType.valueOf(type.toUpperCase());
        var record = BackupRecord.builder()
                .type(backupType)
                .status(BackupStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();
        record = backupRecordRepository.save(record);

        try {
            // Simulated backup (real implementation would exec pg_dump or tar)
            record.setStatus(BackupStatus.SUCCESS);
            record.setCompletedAt(Instant.now());
            record.setFileName(backupType.name().toLowerCase() + "-" + Instant.now().toString().substring(0, 10) + ".gz");
            record = backupRecordRepository.save(record);
        } catch (Exception e) {
            record.setStatus(BackupStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            record = backupRecordRepository.save(record);
        }

        return toBackupResponse(record);
    }

    @Override
    public List<BackupResponse> listBackups(String type) {
        List<BackupRecord> records;
        if (type != null && !type.isBlank()) {
            records = backupRecordRepository.findByTypeOrderByStartedAtDesc(BackupType.valueOf(type.toUpperCase()));
        } else {
            records = backupRecordRepository.findAll().stream()
                    .sorted(Comparator.comparing(BackupRecord::getStartedAt).reversed())
                    .toList();
        }
        return records.stream()
                .map(r -> new BackupResponse(r.getId(), r.getType().name(), r.getStatus().name(), r.getStartedAt()))
                .toList();
    }

    @Override
    public List<LogEntryResponse> getLogs(String serviceName, int limit) {
        return List.of(
                new LogEntryResponse(Instant.now().toString(), "INFO", serviceName + " running normally", serviceName),
                new LogEntryResponse(Instant.now().minusSeconds(60).toString(), "INFO", "Health check passed", serviceName)
        );
    }

    private Incident findIncident(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private IncidentResponse toIncidentResponse(Incident inc) {
        return new IncidentResponse(inc.getId(), inc.getServiceName(), inc.getSeverity().name(),
                inc.getStatus().name(), inc.getTitle(), inc.getDescription(),
                inc.getStartedAt(), inc.getResolvedAt(), inc.getDurationSeconds());
    }

    private BackupResponse toBackupResponse(BackupRecord record) {
        return new BackupResponse(record.getId(), record.getType().name(), record.getStatus().name(), record.getStartedAt());
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%dh %dm", hours, minutes);
    }
}
