package com.amg.digitalitzacio.domains.application;

import com.amg.digitalitzacio.domains.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

// Interfície de servei per al mòdul de gestió de dominis (Mòdul 23)
public interface DomainService {

    // Consulta la disponibilitat d'un domini i el seu preu de venda
    DomainCheckResponse checkAvailability(String domainName);

    // Registra un nou domini via el proveïdor
    ManagedDomainResponse registerDomain(RegisterDomainRequest request);

    // Obté els detalls d'un domini per ID
    ManagedDomainResponse getDomain(UUID id);

    // Llista tots els dominis d'un tenent
    List<ManagedDomainResponse> listByTenant(UUID tenantId);

    // Llista tots els dominis del sistema (paginat, ordenats per data de creació desc)
    Page<ManagedDomainResponse> listAll(int page, int size);

    // Renova un domini per un any addicional
    ManagedDomainResponse renewDomain(UUID id);

    // Configura el DNS del domini apuntant al servidor principal
    ManagedDomainResponse configureDns(UUID id, String serverIp);

    // Actualitza els registres DNS d'un domini
    ManagedDomainResponse updateDnsRecords(UUID id, List<DnsRecordRequest> records);

    // Cancel·la un domini
    void cancelDomain(UUID id);

    // Llista les tarifes de tots els TLDs actius
    List<TldPricingResponse> listTldPricing();

    // Actualitza o crea les tarifes d'un TLD
    TldPricingResponse updateTldPricing(String tld, TldPricingRequest request);

    // Llista dominis que caduquen en els propers N dies
    List<ManagedDomainResponse> listExpiring(int daysAhead);
}
