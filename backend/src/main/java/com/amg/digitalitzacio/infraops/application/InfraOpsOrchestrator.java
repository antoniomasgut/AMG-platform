package com.amg.digitalitzacio.infraops.application;

import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.infraops.api.dto.InfraStatusResponse;
import com.amg.digitalitzacio.infraops.api.dto.MetricSnapshotResponse;
import com.amg.digitalitzacio.infraops.api.dto.RecommendationResponse;
import com.amg.digitalitzacio.infraops.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.notification.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfraOpsOrchestrator implements InfraOpsService {

    private final SystemMetricsCollector metricsCollector;
    private final InfraMetricSnapshotRepository snapshotRepository;
    private final ScalingRecommendationRepository recommendationRepository;
    private final TenantRepository tenantRepository;
    private final TelegramNotifier telegramNotifier;
    private final com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService systemConfigService;

    @Value("${app.infraops.thresholds.cpu-warn:80}") private double cpuWarn;
    @Value("${app.infraops.thresholds.cpu-crit:95}") private double cpuCrit;
    @Value("${app.infraops.thresholds.ram-warn:85}") private double ramWarn;
    @Value("${app.infraops.thresholds.ram-crit:95}") private double ramCrit;
    @Value("${app.infraops.thresholds.disk-warn:75}") private double diskWarn;
    @Value("${app.infraops.thresholds.disk-crit:90}") private double diskCrit;
    @Value("${app.infraops.thresholds.db-connections-warn:80}") private double dbConnWarn;
    @Value("${app.infraops.thresholds.tenants-n8n-warn:30}") private int tenantsN8nWarn;
    @Value("${app.infraops.thresholds.tenants-cloud-warn:50}") private int tenantsCloudWarn;
    @Value("${app.infraops.notification-cooldown-hours:24}") private int cooldownHours;
    @Value("${app.infraops.retention-days:7}") private int retentionDays;

    @Override
    @Transactional(readOnly = true)
    public InfraStatusResponse getStatus() {
        var latest = snapshotRepository.findTopByOrderByCollectedAtDesc();
        if (latest.isEmpty()) {
            return collectNow();
        }
        return toStatusResponse(latest.get());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MetricSnapshotResponse> getMetrics(int hours, int page, int size) {
        var after = Instant.now().minus(hours, ChronoUnit.HOURS);
        return snapshotRepository.findByCollectedAtAfterOrderByCollectedAtDesc(after, PageRequest.of(page, size))
                .map(this::toSnapshotResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getActiveRecommendations() {
        return recommendationRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toRecommendationResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationHistory() {
        return recommendationRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toRecommendationResponse).toList();
    }

    @Override
    @Transactional
    public RecommendationResponse resolveRecommendation(UUID id) {
        var rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + id));
        rec.setResolvedAt(Instant.now());
        rec.setIsActive(false);
        return toRecommendationResponse(recommendationRepository.save(rec));
    }

    @Override
    @Transactional
    public void deleteRecommendation(UUID id) {
        if (!recommendationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recommendation not found: " + id);
        }
        recommendationRepository.deleteById(id);
    }

    @Override
    @Transactional
    public InfraStatusResponse collectNow() {
        var metrics = metricsCollector.collect();
        int activeTenants = (int) tenantRepository.count();

        var snapshot = InfraMetricSnapshot.builder()
                .collectedAt(Instant.now())
                .cpuPercent(metrics.cpuPercent())
                .ramPercent(metrics.ramPercent())
                .diskPercent(metrics.diskPercent())
                .ramUsedMb(metrics.ramUsedMb())
                .ramTotalMb(metrics.ramTotalMb())
                .diskUsedGb(metrics.diskUsedGb())
                .diskTotalGb(metrics.diskTotalGb())
                .dbActiveConnections(metrics.dbActiveConnections())
                .dbMaxConnections(metrics.dbMaxConnections())
                .activeTenants(activeTenants)
                .build();
        snapshot = snapshotRepository.save(snapshot);

        evaluateAndNotify(snapshot);
        cleanupOldSnapshots();

        return toStatusResponse(snapshot);
    }

    private void evaluateAndNotify(InfraMetricSnapshot latest) {
        var now30mAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        var now15mAgo = Instant.now().minus(15, ChronoUnit.MINUTES);

        // CPU — sustained 30 min
        var avgCpu = snapshotRepository.avgCpuSince(now30mAgo);
        if (avgCpu != null) checkAndNotify(RecommendationType.UPGRADE_CPU, avgCpu, cpuWarn, cpuCrit,
                latest.getActiveTenants(),
                "🔴 <b>Recomanació: UPGRADE_CPU</b>\n\nCPU alta de forma sostinguda durant 30 min: <b>%.1f%%</b> (límit: %.0f%%)\n\n💡 Considera ampliar a 8 vCPU (Hetzner CX31, ~40€/mes)".formatted(avgCpu, cpuWarn));

        // RAM — sustained 15 min
        var avgRam = snapshotRepository.avgRamSince(now15mAgo);
        if (avgRam != null) checkAndNotify(RecommendationType.UPGRADE_RAM, avgRam, ramWarn, ramCrit,
                latest.getActiveTenants(),
                "🔴 <b>Recomanació: UPGRADE_RAM</b>\n\nRAM alta de forma sostinguda durant 15 min: <b>%.1f%%</b> (límit: %.0f%%)\n\n💡 Considera ampliar a 16 GB (CX31→CX41, +10€/mes) o separar la DB a VPS dedicat (~5€/mes)".formatted(avgRam, ramWarn));

        // Disc — immediat
        if (latest.getDiskPercent() != null) checkAndNotify(RecommendationType.UPGRADE_DISK, latest.getDiskPercent(), diskWarn, diskCrit,
                latest.getActiveTenants(),
                "🔴 <b>Recomanació: UPGRADE_DISK</b>\n\nDisc quasi ple: <b>%.1f%%</b> (límit: %.0f%%)\n\n💡 Allibera espai o amplia el volum a Hetzner".formatted(latest.getDiskPercent(), diskWarn));

        // DB connexions — sustained 15 min
        var avgDbPct = snapshotRepository.avgDbConnectionsPercentSince(now15mAgo);
        if (avgDbPct != null) checkAndNotify(RecommendationType.SEPARATE_DB, avgDbPct, dbConnWarn, 95.0,
                latest.getActiveTenants(),
                "🔴 <b>Recomanació: SEPARATE_DB</b>\n\nConnexions DB altes: <b>%.1f%%</b> del pool (límit: %.0f%%)\n\n💡 Considera separar PostgreSQL a un VPS dedicat (Hetzner CX11, ~5€/mes)".formatted(avgDbPct, dbConnWarn));

        // Tenants — count-based
        int tenants = latest.getActiveTenants() != null ? latest.getActiveTenants() : 0;
        if (tenants >= tenantsCloudWarn) {
            checkAndNotify(RecommendationType.MIGRATE_HETZNER_CLOUD, (double) tenants, (double) tenantsCloudWarn, (double) tenantsCloudWarn,
                    tenants,
                    "💡 <b>Recomanació: MIGRATE_HETZNER_CLOUD</b>\n\n%d tenants actius (límit recomanat: %d)\n\nÉs hora de migrar a Hetzner Cloud amb Load Balancer i DB gestionada".formatted(tenants, tenantsCloudWarn));
        } else if (tenants >= tenantsN8nWarn) {
            checkAndNotify(RecommendationType.SEPARATE_N8N, (double) tenants, (double) tenantsN8nWarn, (double) tenantsN8nWarn,
                    tenants,
                    "💡 <b>Recomanació: SEPARATE_N8N</b>\n\n%d tenants actius (límit recomanat: %d)\n\nConsulta separar n8n a un VPS dedicat per alliberar recursos".formatted(tenants, tenantsN8nWarn));
        }
    }

    /** Tipus de recomanació desactivats via SystemConfig INFRAOPS_DISABLED_RECS (noms separats per coma). */
    private boolean isRecommendationDisabled(RecommendationType type) {
        String disabled = systemConfigService.get("INFRAOPS_DISABLED_RECS");
        if (disabled == null || disabled.isBlank()) return false;
        return java.util.Arrays.stream(disabled.split(","))
                .map(String::trim)
                .anyMatch(t -> t.equalsIgnoreCase(type.name()));
    }

    private void checkAndNotify(RecommendationType type, double value, double warn, double crit,
                                 int activeTenants, String messageTemplate) {
        // Tipus de recomanació desactivats per config (INFRAOPS_DISABLED_RECS, separats per coma).
        // Ex: desactivar SEPARATE_N8N si no s'usa n8n.
        if (isRecommendationDisabled(type)) return;
        if (value < warn) {
            // Si estava actiu, marcar com resolt
            recommendationRepository.findTopByTypeOrderByCreatedAtDesc(type)
                    .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                    .ifPresent(r -> {
                        r.setIsActive(false);
                        r.setResolvedAt(Instant.now());
                        recommendationRepository.save(r);
                    });
            return;
        }

        // Cooldown: no enviar si ja n'hem enviat una en les últimes X hores
        var cooldownSince = Instant.now().minus(cooldownHours, ChronoUnit.HOURS);
        if (recommendationRepository.existsByTypeAndSentAtAfter(type, cooldownSince)) {
            return;
        }

        var severity = value >= crit ? RecommendationSeverity.CRITICAL : RecommendationSeverity.WARNING;
        var message = messageTemplate + "\n\n📊 Context: %d tenants actius\n⏰ Pròxima alerta en %dh".formatted(activeTenants, cooldownHours);

        var rec = ScalingRecommendation.builder()
                .type(type)
                .severity(severity)
                .message(message)
                .triggerValue(value)
                .threshold(warn)
                .sentAt(Instant.now())
                .isActive(true)
                .build();
        recommendationRepository.save(rec);

        telegramNotifier.send(message);
        log.info("InfraOps: recomanació {} ({}) enviada — valor: {:.1f}", type, severity, value);
    }

    private void cleanupOldSnapshots() {
        var cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        snapshotRepository.deleteByCollectedAtBefore(cutoff);
    }

    private InfraStatusResponse toStatusResponse(InfraMetricSnapshot s) {
        double cpu = s.getCpuPercent() != null ? s.getCpuPercent() : 0;
        double ram = s.getRamPercent() != null ? s.getRamPercent() : 0;
        double disk = s.getDiskPercent() != null ? s.getDiskPercent() : 0;
        double dbPct = s.getDbMaxConnections() != null && s.getDbMaxConnections() > 0
                ? s.getDbActiveConnections() * 100.0 / s.getDbMaxConnections() : 0;

        int tenants = s.getActiveTenants() != null ? s.getActiveTenants() : 0;
        String tenantRec = tenants >= tenantsCloudWarn ? "MIGRATE_HETZNER_CLOUD"
                : tenants >= tenantsN8nWarn ? "SEPARATE_N8N" : null;

        int activeRecs = (int) recommendationRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive())).count();

        String overall = activeRecs > 0 ? "WARNING" : "OK";

        return new InfraStatusResponse(
                s.getCollectedAt(),
                new InfraStatusResponse.CpuStatus(round(cpu), statusStr(cpu, cpuWarn, cpuCrit)),
                new InfraStatusResponse.RamStatus(
                        s.getRamUsedMb() != null ? s.getRamUsedMb() : 0,
                        s.getRamTotalMb() != null ? s.getRamTotalMb() : 0,
                        round(ram), statusStr(ram, ramWarn, ramCrit)),
                new InfraStatusResponse.DiskStatus(
                        s.getDiskUsedGb() != null ? s.getDiskUsedGb() : 0,
                        s.getDiskTotalGb() != null ? s.getDiskTotalGb() : 0,
                        round(disk), statusStr(disk, diskWarn, diskCrit)),
                new InfraStatusResponse.DbStatus(
                        s.getDbActiveConnections() != null ? s.getDbActiveConnections() : 0,
                        s.getDbMaxConnections() != null ? s.getDbMaxConnections() : 0,
                        round(dbPct), statusStr(dbPct, dbConnWarn, 95)),
                new InfraStatusResponse.TenantStatus(tenants, tenantRec),
                overall,
                activeRecs
        );
    }

    private String statusStr(double value, double warn, double crit) {
        return value >= crit ? "CRITICAL" : value >= warn ? "WARNING" : "OK";
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private MetricSnapshotResponse toSnapshotResponse(InfraMetricSnapshot s) {
        return new MetricSnapshotResponse(
                s.getId(), s.getCollectedAt(),
                s.getCpuPercent() != null ? round(s.getCpuPercent()) : 0,
                s.getRamPercent() != null ? round(s.getRamPercent()) : 0,
                s.getDiskPercent() != null ? round(s.getDiskPercent()) : 0,
                s.getRamUsedMb() != null ? s.getRamUsedMb() : 0,
                s.getRamTotalMb() != null ? s.getRamTotalMb() : 0,
                s.getDiskUsedGb() != null ? s.getDiskUsedGb() : 0,
                s.getDiskTotalGb() != null ? s.getDiskTotalGb() : 0,
                s.getDbActiveConnections() != null ? s.getDbActiveConnections() : 0,
                s.getDbMaxConnections() != null ? s.getDbMaxConnections() : 0,
                s.getActiveTenants() != null ? s.getActiveTenants() : 0
        );
    }

    private RecommendationResponse toRecommendationResponse(ScalingRecommendation r) {
        return new RecommendationResponse(
                r.getId(), r.getType().name(), r.getSeverity().name(),
                r.getMessage(), r.getTriggerValue(), r.getThreshold(),
                r.getSentAt(), r.getResolvedAt(),
                Boolean.TRUE.equals(r.getIsActive()),
                r.getCreatedAt());
    }
}
