package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.auth.application.PhaseHealthService;
import com.amg.digitalitzacio.auth.domain.TenantPhaseActivationRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.UUID;

/**
 * Vigilant de fases mortes: una fase activada fa més de 7 dies amb la salut
 * en vermell (config no creada, grup Telegram absent...) és una fase que el
 * client paga i que no fa res. Alerta única per tenant+fase al xat de vendes.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class PhaseConfigWatchdogScheduler {

    private static final int GRACE_DAYS = 7;

    private final TenantPhaseActivationRepository activationRepository;
    private final TenantRepository tenantRepository;
    private final PhaseHealthService phaseHealthService;
    private final FollowupLogRepository followupLogRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;

    @Scheduled(cron = "0 15 9 * * *")
    public void checkUnconfiguredPhases() {
        var cutoff = Instant.now().minus(GRACE_DAYS, ChronoUnit.DAYS);
        var activations = activationRepository.findByDeactivatedAtIsNullAndActivatedAtBefore(cutoff);
        if (activations.isEmpty()) return;

        var seenTenants = new HashSet<UUID>();
        int alerts = 0;
        for (var activation : activations) {
            UUID tenantId = activation.getTenantId();
            if (!seenTenants.add(tenantId)) continue; // salut per tenant, un cop

            var tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null || Boolean.TRUE.equals(tenant.getIsOwner())) continue;

            try {
                var health = phaseHealthService.checkHealth(tenantId);
                for (var phase : health.phaseHealth()) {
                    if (phase.healthy()) continue;
                    if (!tenant.isPhaseActive(phase.phase())) continue;

                    String logType = "NOCONFIG_" + phase.phase(); // p. ex. NOCONFIG_F4
                    if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(tenantId, logType, tenantId)) {
                        continue;
                    }
                    sendAlert(tenant.getName(), tenantId, phase.phase(), String.join("; ", phase.issues()));
                    var entry = new FollowupLog();
                    entry.setTenantId(tenantId);
                    entry.setType(logType);
                    entry.setEntityId(tenantId);
                    followupLogRepository.save(entry);
                    alerts++;
                }
            } catch (Exception e) {
                log.warn("[Watchdog] Error comprovant fases del tenant {}: {}", tenantId, e.getMessage());
            }
        }
        if (alerts > 0) log.info("[Watchdog] {} alertes de fases sense configurar enviades", alerts);
    }

    private void sendAlert(String tenantName, UUID tenantId, String phase, String issues) {
        try {
            String chatIdStr = sysConfig.get("AMG_SALES_CHAT_ID");
            if (chatIdStr == null || chatIdStr.isBlank()) return;
            String msg = """
                    🧟 <b>Fase pagada però inactiva</b> — %s
                    Fase %s activada fa més de %d dies sense configurar:
                    %s
                    🔗 <a href="https://amgdl.com/portal/admin/tenants/%s/setup">Configurar →</a>
                    """.formatted(tenantName != null ? tenantName : "—", phase, GRACE_DAYS, issues, tenantId);
            telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), msg);
        } catch (Exception e) {
            log.warn("[Watchdog] Error enviant alerta: {}", e.getMessage());
        }
    }
}
