package com.amg.digitalitzacio.metaads.application;

import com.amg.digitalitzacio.metaads.domain.AdCampaignRepository;
import com.amg.digitalitzacio.metaads.domain.CampaignStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetaAdsStatusSyncJob {

    private final AdCampaignRepository campaignRepository;
    private final MetaAdsPublishService publishService;

    // Cada hora comprova campanyes en revisió
    @Scheduled(cron = "0 0 * * * *")
    public void syncPendingReview() {
        var campaigns = campaignRepository.findByStatusIn(
                List.of(CampaignStatus.PENDING_REVIEW));
        if (campaigns.isEmpty()) return;

        log.info("[MetaAds] Sync d'estats: {} campanyes en PENDING_REVIEW", campaigns.size());
        for (var campaign : campaigns) {
            publishService.syncStatus(campaign);
        }
    }

    // Cada dia a les 07:30 UTC sincronitza totes les actives
    @Scheduled(cron = "0 30 7 * * *")
    public void syncAllActive() {
        var campaigns = campaignRepository.findByStatusIn(
                List.of(CampaignStatus.ACTIVE, CampaignStatus.PENDING_REVIEW));
        for (var campaign : campaigns) {
            publishService.syncStatus(campaign);
        }
    }
}
