package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.ProspectCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final ProspectCampaignRepository campaignRepository;
    private final ProspectingOrchestrator orchestrator;

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void executeScheduledCampaigns() {
        var due = campaignRepository.findScheduledDue(Instant.now());
        if (due.isEmpty()) return;
        log.info("Executing {} scheduled campaigns", due.size());
        for (var campaign : due) {
            try {
                campaign.setStatus(com.amg.digitalitzacio.prospecting.domain.CampaignStatus.IN_PROGRESS);
                campaignRepository.save(campaign);
                orchestrator.runCampaign(campaign.getId());
                if (campaign.getRepeatIntervalDays() != null && campaign.getRepeatIntervalDays() > 0) {
                    campaign.setScheduledNextRun(Instant.now().plus(java.time.Duration.ofDays(campaign.getRepeatIntervalDays())));
                    campaign.setStatus(com.amg.digitalitzacio.prospecting.domain.CampaignStatus.SCHEDULED);
                } else {
                    campaign.setScheduledNextRun(null);
                    campaign.setStatus(com.amg.digitalitzacio.prospecting.domain.CampaignStatus.COMPLETED);
                }
                campaignRepository.save(campaign);
                log.info("Scheduled campaign {} executed successfully", campaign.getId());
            } catch (Exception e) {
                log.error("Error executing scheduled campaign {}: {}", campaign.getId(), e.getMessage());
                campaign.setStatus(com.amg.digitalitzacio.prospecting.domain.CampaignStatus.FAILED);
                campaignRepository.save(campaign);
            }
        }
    }
}
