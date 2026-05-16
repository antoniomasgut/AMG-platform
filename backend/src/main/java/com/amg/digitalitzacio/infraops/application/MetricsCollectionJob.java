package com.amg.digitalitzacio.infraops.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class MetricsCollectionJob {

    private final InfraOpsService infraOpsService;

    // Cada 5 minuts: seconds=0, minutes=*/5
    @Scheduled(cron = "${app.infraops.collect-cron:0 */5 * * * ?}")
    public void collect() {
        try {
            infraOpsService.collectNow();
            log.debug("InfraOps: snapshot de mètriques recollit");
        } catch (Exception e) {
            log.error("InfraOps: error recollint mètriques: {}", e.getMessage(), e);
        }
    }
}
