package com.amg.digitalitzacio.metaads.application;

import com.amg.digitalitzacio.metaads.domain.MetaAdsConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetaAdsSyncJob {

    private final MetaAdsConfigRepository configRepository;
    private final MetaAdsService metaAdsService;

    // Executa cada dia a les 06:00 UTC (dades de "yesterday" de Meta)
    @Scheduled(cron = "0 0 6 * * *")
    public void syncAll() {
        var configs = configRepository.findByEnabledTrue();
        if (configs.isEmpty()) return;

        log.info("[MetaAds] Sync diari: {} tenants amb Meta Ads activat", configs.size());
        int total = 0;
        for (var config : configs) {
            try {
                total += metaAdsService.fetchAndStore(config);
            } catch (Exception e) {
                log.error("[MetaAds] Error al sync del tenant {}: {}", config.getTenantId(), e.getMessage());
            }
        }
        log.info("[MetaAds] Sync diari completat: {} registres nous", total);
    }
}
