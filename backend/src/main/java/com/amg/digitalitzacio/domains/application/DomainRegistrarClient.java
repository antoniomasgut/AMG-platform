package com.amg.digitalitzacio.domains.application;

import com.amg.digitalitzacio.domains.domain.DomainDnsRecord;

import java.util.List;

// Port d'integració amb el proveïdor de registre de dominis (OpenProvider o mock)
public interface DomainRegistrarClient {

    // Consulta si un domini és disponible per a registre
    boolean checkAvailability(String domainName);

    // Suggereix alternatives si el domini no és disponible
    List<String> suggestAlternatives(String domainName);

    // Registra un domini i retorna l'ID intern del proveïdor
    String registerDomain(String domainName, String registrantName, String registrantEmail,
                          String registrantPhone, String registrantNif);

    // Renova un domini per un any addicional
    void renewDomain(String providerDomainId);

    // Configura els registres DNS al proveïdor
    void setDnsRecords(String providerDomainId, List<DomainDnsRecord> records);

    // Cancel·la un domini (no renovarà en caducar)
    void cancelDomain(String providerDomainId);
}
