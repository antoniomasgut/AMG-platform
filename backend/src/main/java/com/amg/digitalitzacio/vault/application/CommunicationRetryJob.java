package com.amg.digitalitzacio.vault.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationRetryJob {

    private final VaultCommunicationService vaultCommunicationService;

    @Scheduled(fixedRate = 3_600_000)
    public void retryExpiredCommunications() {
        var retried = vaultCommunicationService.retryExpired();
        if (!retried.isEmpty()) {
            log.info("Retried {} expired communication requests", retried.size());
        }
    }
}
