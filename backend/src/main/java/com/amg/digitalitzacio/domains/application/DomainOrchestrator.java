package com.amg.digitalitzacio.domains.application;

import com.amg.digitalitzacio.domains.api.dto.*;
import com.amg.digitalitzacio.domains.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Orquestrador del mòdul de dominis: coordina el proveïdor de registre amb la persistència local
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainOrchestrator implements DomainService {

    private final ManagedDomainRepository domainRepository;
    private final DomainDnsRecordRepository dnsRecordRepository;
    private final TldPricingRepository tldPricingRepository;
    private final DomainRegistrarClient registrarClient;

    // IP del servidor principal on s'apunten els dominis gestionats
    @Value("${app.domains.server-ip:65.108.148.62}")
    private String serverIp;

    @Override
    @Transactional(readOnly = true)
    public DomainCheckResponse checkAvailability(String domainName) {
        // Extreu el TLD de la part final del nom de domini
        var tld = extractTld(domainName);
        var pricingOpt = tldPricingRepository.findById(tld);

        boolean available = registrarClient.checkAvailability(domainName);
        List<String> alternatives = available
                ? List.of()
                : registrarClient.suggestAlternatives(domainName);

        var saleRegister = pricingOpt.map(TldPricing::getSaleRegister).orElse(java.math.BigDecimal.ZERO);
        var saleRenew = pricingOpt.map(TldPricing::getSaleRenew).orElse(java.math.BigDecimal.ZERO);

        return new DomainCheckResponse(domainName, available, saleRegister, saleRenew, alternatives);
    }

    @Override
    @Transactional
    public ManagedDomainResponse registerDomain(RegisterDomainRequest request) {
        // Comprova que el domini no existeixi ja al sistema
        if (domainRepository.existsByDomainName(request.domainName())) {
            throw new IllegalArgumentException("El domini ja existeix al sistema: " + request.domainName());
        }

        // Comprova la disponibilitat real al proveïdor
        if (!registrarClient.checkAvailability(request.domainName())) {
            throw new IllegalArgumentException("El domini no està disponible per a registre: " + request.domainName());
        }

        var tld = extractTld(request.domainName());
        var pricingOpt = tldPricingRepository.findById(tld);

        // Registra el domini al proveïdor i obté l'ID intern
        var providerDomainId = registrarClient.registerDomain(
                request.domainName(),
                request.registrantName(),
                request.registrantEmail(),
                request.registrantPhone(),
                request.registrantNif());

        // Crea l'entitat amb estat REGISTERING
        var domain = ManagedDomain.builder()
                .tenantId(request.tenantId())
                .landingId(request.landingId())
                .domainName(request.domainName())
                .tld(tld)
                .status(DomainStatus.REGISTERING)
                .providerDomainId(providerDomainId)
                .registrantName(request.registrantName())
                .registrantEmail(request.registrantEmail())
                .registrantPhone(request.registrantPhone())
                .registrantNif(request.registrantNif())
                .autoRenew(request.autoRenew())
                .registeredAt(Instant.now())
                .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
                .purchasePrice(pricingOpt.map(TldPricing::getCostRegister).orElse(null))
                .salePrice(pricingOpt.map(TldPricing::getSaleRegister).orElse(null))
                .build();

        domain = domainRepository.save(domain);

        // Actualitza l'estat a ACTIVE un cop el proveïdor ha confirmat el registre
        domain.setStatus(DomainStatus.ACTIVE);
        domain = domainRepository.save(domain);

        // Si hi ha landing associada, configura el DNS automàticament
        if (request.landingId() != null) {
            log.info("Landing {} associada al domini {}, configurant DNS automàticament",
                    request.landingId(), request.domainName());
            domain = configureDnsInternal(domain, serverIp);
        }

        return toResponse(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public ManagedDomainResponse getDomain(UUID id) {
        var domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domini no trobat: " + id));
        return toResponse(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedDomainResponse> listByTenant(UUID tenantId) {
        return domainRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagedDomainResponse> listAll(int page, int size) {
        return domainRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ManagedDomainResponse renewDomain(UUID id) {
        var domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domini no trobat: " + id));

        registrarClient.renewDomain(domain.getProviderDomainId());

        // Afegeix un any a la data de caducitat
        var currentExpiry = domain.getExpiresAt() != null ? domain.getExpiresAt() : Instant.now();
        domain.setExpiresAt(currentExpiry.plus(365, ChronoUnit.DAYS));
        domain.setStatus(DomainStatus.ACTIVE);
        domain = domainRepository.save(domain);

        log.info("Domini {} renovat. Nova data de caducitat: {}", domain.getDomainName(), domain.getExpiresAt());
        return toResponse(domain);
    }

    @Override
    @Transactional
    public ManagedDomainResponse configureDns(UUID id, String ip) {
        var domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domini no trobat: " + id));
        var effectiveIp = (ip != null && !ip.isBlank()) ? ip : serverIp;
        domain = configureDnsInternal(domain, effectiveIp);
        return toResponse(domain);
    }

    @Override
    @Transactional
    public ManagedDomainResponse updateDnsRecords(UUID id, List<DnsRecordRequest> records) {
        var domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domini no trobat: " + id));

        // Elimina els registres DNS actuals i en crea de nous
        dnsRecordRepository.deleteByDomainId(id);

        var newRecords = records.stream()
                .map(r -> DomainDnsRecord.builder()
                        .domainId(id)
                        .type(r.type())
                        .name(r.name())
                        .value(r.value())
                        .ttl(r.ttl())
                        .priority(r.priority())
                        .build())
                .collect(Collectors.toList());

        dnsRecordRepository.saveAll(newRecords);

        // Actualitza al proveïdor
        registrarClient.setDnsRecords(domain.getProviderDomainId(), newRecords);

        domain.setDnsConfigured(true);
        domain = domainRepository.save(domain);

        log.info("DNS del domini {} actualitzat amb {} registres", domain.getDomainName(), newRecords.size());
        return toResponse(domain);
    }

    @Override
    @Transactional
    public void cancelDomain(UUID id) {
        var domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domini no trobat: " + id));

        registrarClient.cancelDomain(domain.getProviderDomainId());
        domain.setStatus(DomainStatus.CANCELLED);
        domainRepository.save(domain);

        log.info("Domini {} cancel·lat", domain.getDomainName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TldPricingResponse> listTldPricing() {
        return tldPricingRepository.findByIsActiveTrue()
                .stream()
                .map(this::toTldResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TldPricingResponse updateTldPricing(String tld, TldPricingRequest request) {
        // Actualitza si existeix, o crea nou si no
        var pricing = tldPricingRepository.findById(tld)
                .orElseGet(() -> TldPricing.builder().tld(tld).build());

        pricing.setSaleRegister(request.saleRegister());
        pricing.setSaleRenew(request.saleRenew());
        pricing.setActive(request.isActive());
        pricing = tldPricingRepository.save(pricing);

        log.info("Tarifa del TLD {} actualitzada: saleRegister={}, saleRenew={}",
                tld, request.saleRegister(), request.saleRenew());
        return toTldResponse(pricing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedDomainResponse> listExpiring(int daysAhead) {
        var cutoff = Instant.now().plus(daysAhead, ChronoUnit.DAYS);
        return domainRepository.findByExpiresAtBeforeAndAutoRenewTrue(cutoff)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers privats ──

    // Configura els registres DNS bàsics (@ i www apuntant a serverIp) per a un domini
    private ManagedDomain configureDnsInternal(ManagedDomain domain, String ip) {
        dnsRecordRepository.deleteByDomainId(domain.getId());

        var recordRoot = DomainDnsRecord.builder()
                .domainId(domain.getId())
                .type("A")
                .name("@")
                .value(ip)
                .ttl(3600)
                .build();

        var recordWww = DomainDnsRecord.builder()
                .domainId(domain.getId())
                .type("A")
                .name("www")
                .value(ip)
                .ttl(3600)
                .build();

        var records = List.of(recordRoot, recordWww);
        dnsRecordRepository.saveAll(records);
        registrarClient.setDnsRecords(domain.getProviderDomainId(), records);

        domain.setDnsConfigured(true);
        domain.setStatus(DomainStatus.ACTIVE);
        domain = domainRepository.save(domain);

        log.info("DNS del domini {} configurat apuntant a {}", domain.getDomainName(), ip);
        return domain;
    }

    // Extreu el TLD de la darrera part del nom de domini (p.ex. "cat" de "negoci.cat")
    private String extractTld(String domainName) {
        var parts = domainName.split("\\.");
        return parts[parts.length - 1];
    }

    // Mapeja una entitat ManagedDomain al DTO de resposta incloent els registres DNS
    private ManagedDomainResponse toResponse(ManagedDomain domain) {
        var dnsRecords = dnsRecordRepository.findByDomainId(domain.getId())
                .stream()
                .map(r -> new DnsRecordResponse(r.getId(), r.getType(), r.getName(),
                        r.getValue(), r.getTtl(), r.getPriority()))
                .collect(Collectors.toList());

        return new ManagedDomainResponse(
                domain.getId(),
                domain.getTenantId(),
                domain.getDomainName(),
                domain.getTld(),
                domain.getStatus().name(),
                domain.getPurchasePrice(),
                domain.getSalePrice(),
                domain.getRegisteredAt(),
                domain.getExpiresAt(),
                domain.isAutoRenew(),
                domain.isDnsConfigured(),
                domain.getProvider(),
                dnsRecords
        );
    }

    // Mapeja una entitat TldPricing al DTO de resposta
    private TldPricingResponse toTldResponse(TldPricing pricing) {
        return new TldPricingResponse(
                pricing.getTld(),
                pricing.getCostRegister(),
                pricing.getCostRenew(),
                pricing.getSaleRegister(),
                pricing.getSaleRenew(),
                pricing.isActive()
        );
    }
}
