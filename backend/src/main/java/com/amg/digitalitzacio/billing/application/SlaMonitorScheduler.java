package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

// Comprova cada hora si hi ha setups completats sense activar en més de 24h
// i fitxes de configuració que el client no ha omplert en més de 48h
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaMonitorScheduler {

    private static final String LOG_TYPE = "SLA_SETUP_EXPIRED";
    private static final String LOG_TYPE_INTAKE = "INTAKE_PENDING_48H";
    private static final long SLA_SETUP_TO_ACTIVE_HOURS = 24;
    private static final long INTAKE_PENDING_HOURS = 48;

    private final BudgetSetupIntakeRepository intakeRepository;
    private final TenantRepository tenantRepository;
    private final PostAcceptanceService postAcceptanceService;
    private final FollowupLogRepository followupLogRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void checkSetupCompletedSla() {
        Instant deadline = Instant.now().minus(SLA_SETUP_TO_ACTIVE_HOURS, ChronoUnit.HOURS);

        var overdueIntakes = intakeRepository.findByStatusAndCompletedAtBefore("COMPLETE", deadline);

        for (var intake : overdueIntakes) {
            try {
                var tenant = tenantRepository.findById(intake.getTenantId()).orElse(null);
                boolean alreadyActive = false;
                if (tenant != null && tenant.getContractedPhases() != null && !tenant.getContractedPhases().isBlank()) {
                    var contracted = Set.of(tenant.getContractedPhases().split(","));
                    var active = tenant.getActivePhases() != null
                            ? Set.of(tenant.getActivePhases().split(","))
                            : contracted;
                    alreadyActive = active.containsAll(contracted);
                } else if (tenant != null) {
                    alreadyActive = tenant.getActivePhases() != null && !tenant.getActivePhases().isBlank();
                }

                if (alreadyActive) continue;

                // Una sola alerta per intake — evita spam cada hora
                if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(
                        intake.getTenantId(), LOG_TYPE, intake.getId())) continue;

                long hoursWaiting = ChronoUnit.HOURS.between(intake.getCompletedAt(), Instant.now());
                log.warn("[SLA] Setup completat fa {}h sense activar — tenant {}", hoursWaiting, intake.getTenantId());
                postAcceptanceService.onSetupSlaExpired(intake, hoursWaiting);

                var log2 = new FollowupLog();
                log2.setTenantId(intake.getTenantId());
                log2.setType(LOG_TYPE);
                log2.setEntityId(intake.getId());
                followupLogRepository.save(log2);
            } catch (Exception e) {
                log.error("[SLA] Error processant intake {} (setup completat): {}", intake.getId(), e.getMessage());
            }
        }
    }

    /**
     * Fitxes de configuració que el client no ha omplert en 48h des de l'acceptació:
     * recordatori al client (email) + alerta a l'equip AMG (Telegram). Un sol avís per intake.
     */
    @Scheduled(cron = "0 30 * * * *")
    public void checkIntakePendingSla() {
        Instant deadline = Instant.now().minus(INTAKE_PENDING_HOURS, ChronoUnit.HOURS);
        var pending = intakeRepository.findByStatusInAndCreatedAtBefore(
                java.util.List.of("PENDING", "IN_PROGRESS"), deadline);

        for (var intake : pending) {
            try {
                if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(
                        intake.getTenantId(), LOG_TYPE_INTAKE, intake.getId())) continue;

                long hoursWaiting = ChronoUnit.HOURS.between(intake.getCreatedAt(), Instant.now());
                log.warn("[SLA] Fitxa de configuració sense omplir fa {}h — tenant {}", hoursWaiting, intake.getTenantId());
                postAcceptanceService.onIntakePending(intake, hoursWaiting);

                var entry = new FollowupLog();
                entry.setTenantId(intake.getTenantId());
                entry.setType(LOG_TYPE_INTAKE);
                entry.setEntityId(intake.getId());
                followupLogRepository.save(entry);
            } catch (Exception e) {
                log.error("[SLA] Error processant intake {} (pendent): {}", intake.getId(), e.getMessage());
            }
        }
    }
}
