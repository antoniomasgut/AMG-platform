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

// Comprova cada hora si hi ha setups completats sense activar en més de 24h
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaMonitorScheduler {

    private static final String LOG_TYPE = "SLA_SETUP_EXPIRED";
    private static final long SLA_SETUP_TO_ACTIVE_HOURS = 24;

    private final BudgetSetupIntakeRepository intakeRepository;
    private final TenantRepository tenantRepository;
    private final PostAcceptanceService postAcceptanceService;
    private final FollowupLogRepository followupLogRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void checkSetupCompletedSla() {
        Instant deadline = Instant.now().minus(SLA_SETUP_TO_ACTIVE_HOURS, ChronoUnit.HOURS);

        var overdueIntakes = intakeRepository.findByStatusAndCompletedAtBefore("COMPLETE", deadline);

        for (var intake : overdueIntakes) {
            var tenant = tenantRepository.findById(intake.getTenantId()).orElse(null);
            boolean alreadyActive = tenant != null
                    && tenant.getActivePhases() != null
                    && !tenant.getActivePhases().isBlank();

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
        }
    }
}
