package com.amg.digitalitzacio.documents.delivery.application;

import com.amg.digitalitzacio.documents.delivery.domain.SecureDocumentTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecureDocumentCleanupJob {

    private final SecureDocumentTokenRepository tokenRepo;

    // Cada dia a les 03:00 UTC
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        // STANDARD: elimina tokens expirats fa més de 30 dies
        Instant standardCutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int std = tokenRepo.deleteExpiredStandard(standardCutoff);

        // SENSITIVE: elimina tokens expirats fa més de 90 dies (retenció auditoria RGPD)
        Instant sensitiveCutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        int sens = tokenRepo.deleteExpiredSensitive(sensitiveCutoff);

        if (std > 0 || sens > 0) {
            log.info("[SecureDoc] Cleanup: {} STANDARD + {} SENSITIVE tokens eliminats", std, sens);
        }
    }
}
