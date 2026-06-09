package com.amg.digitalitzacio.domains.application;

import com.amg.digitalitzacio.domains.domain.DomainDnsRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// Client mock del proveïdor de registre de dominis — només actiu en tests
@Slf4j
@Component
@Profile("test")
public class MockDomainRegistrarClient implements DomainRegistrarClient {

    @Override
    public boolean checkAvailability(String domainName) {
        // Simula dominis no disponibles si acaben en "taken.com" o "taken.cat"
        boolean available = !domainName.endsWith("taken.com") && !domainName.endsWith("taken.cat");
        log.info("[MOCK] checkAvailability: {} -> {}", domainName, available);
        return available;
    }

    @Override
    public List<String> suggestAlternatives(String domainName) {
        // Suggereix variants amb sufix "-pro" i "-online"
        var alternatives = List.of(
                domainName.replace(".", "-pro."),
                domainName.replace(".", "-online.")
        );
        log.info("[MOCK] suggestAlternatives per {}: {}", domainName, alternatives);
        return alternatives;
    }

    @Override
    public String registerDomain(String domainName, String registrantName, String registrantEmail,
                                  String registrantPhone, String registrantNif) {
        var mockId = "MOCK-" + UUID.randomUUID();
        log.info("[MOCK] registerDomain: {} -> providerDomainId={}", domainName, mockId);
        return mockId;
    }

    @Override
    public void renewDomain(String providerDomainId) {
        log.info("[MOCK] renewDomain: providerDomainId={}", providerDomainId);
    }

    @Override
    public void setDnsRecords(String providerDomainId, List<DomainDnsRecord> records) {
        log.info("[MOCK] setDnsRecords: providerDomainId={}, registres={}", providerDomainId, records.size());
    }

    @Override
    public void cancelDomain(String providerDomainId) {
        log.info("[MOCK] cancelDomain: providerDomainId={}", providerDomainId);
    }
}
