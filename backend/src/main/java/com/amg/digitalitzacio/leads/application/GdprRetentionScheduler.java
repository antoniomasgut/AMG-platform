package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.leads.domain.Activity;
import com.amg.digitalitzacio.leads.domain.ActivityRepository;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GdprRetentionScheduler {

    private static final int RETENTION_YEARS = 3;

    private final LeadRepository leadRepository;
    private final ActivityRepository activityRepository;

    // Executa cada nit a les 3:30 AM
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredPersonalData() {
        Instant cutoff = Instant.now().minus(RETENTION_YEARS * 365L, ChronoUnit.DAYS);
        List<Lead> expired = leadRepository.findByCreatedAtBefore(cutoff);

        if (expired.isEmpty()) return;

        int purged = 0;
        for (Lead lead : expired) {
            // Salta si ja estava anonimitzat
            if ("[Dades esborrades - RGPD]".equals(lead.getName())) continue;

            activityRepository.deleteByLeadId(lead.getId());
            lead.setName("[Dades esborrades - RGPD]");
            lead.setEmail(null);
            lead.setPhone(null);
            lead.setNotes(null);
            lead.setTags(null);
            lead.setIsActive(false);
            purged++;
        }

        if (purged > 0) {
            leadRepository.saveAll(expired);
            log.info("[RGPD] Purga de retenció completada: {} leads anonimitzats (anteriors a {})", purged, cutoff);
        }
    }
}
