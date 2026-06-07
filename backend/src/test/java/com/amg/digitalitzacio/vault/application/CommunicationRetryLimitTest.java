package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.vault.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que retryExpired() processa màxim 100 registres per execució,
 * evitant problemes de memòria si hi ha molts comunicats expirats.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class CommunicationRetryLimitTest {

    @Autowired private VaultCommunicationService service;
    @Autowired private CommunicationRequestRepository repository;

    @Test
    void retryExpired_processesAtMost100Records() {
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID tid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();

        // Inserim 150 registres expirats
        var requests = IntStream.range(0, 150)
                .mapToObj(i -> CommunicationRequest.builder()
                        .tenantId(tid)
                        .tenantServiceId(sid)
                        .channel(CommunicationChannel.EMAIL)
                        .recipient("client" + i + "@example.com")
                        .subject("Test " + i)
                        .body("Missatge " + i)
                        .status(CommunicationStatus.SENT)
                        .requestType(RequestType.REQUEST_INFO)
                        .expiresAt(pastExpiry)
                        .sentAt(pastExpiry)
                        .retryCount(0)
                        .maxRetries(3)
                        .build())
                .toList();
        repository.saveAll(requests);

        List<CommunicationRequest> processed = service.retryExpired();

        assertThat(processed).hasSize(100);
    }

    @Test
    void retryExpired_ignoresNonExpiredAndMaxedOut() {
        Instant futureExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant pastExpiry  = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID tid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();

        // No expirat → no s'ha de processar
        repository.save(CommunicationRequest.builder()
                .tenantId(tid).tenantServiceId(sid)
                .channel(CommunicationChannel.EMAIL).recipient("ok@ex.com")
                .subject("x").body("y")
                .status(CommunicationStatus.SENT).requestType(RequestType.REQUEST_INFO)
                .expiresAt(futureExpiry).sentAt(pastExpiry).retryCount(0).maxRetries(3)
                .build());

        // Expirat però ja ha esgotat els reintents → no s'ha de processar
        repository.save(CommunicationRequest.builder()
                .tenantId(tid).tenantServiceId(sid)
                .channel(CommunicationChannel.EMAIL).recipient("maxed@ex.com")
                .subject("x").body("y")
                .status(CommunicationStatus.SENT).requestType(RequestType.REQUEST_INFO)
                .expiresAt(pastExpiry).sentAt(pastExpiry).retryCount(3).maxRetries(3)
                .build());

        // Expirat i elegible → s'ha de processar
        repository.save(CommunicationRequest.builder()
                .tenantId(tid).tenantServiceId(sid)
                .channel(CommunicationChannel.EMAIL).recipient("retry@ex.com")
                .subject("x").body("y")
                .status(CommunicationStatus.SENT).requestType(RequestType.REQUEST_INFO)
                .expiresAt(pastExpiry).sentAt(pastExpiry).retryCount(1).maxRetries(3)
                .build());

        List<CommunicationRequest> processed = service.retryExpired();
        assertThat(processed).hasSize(1);
        assertThat(processed.get(0).getRecipient()).isEqualTo("retry@ex.com");
    }
}
