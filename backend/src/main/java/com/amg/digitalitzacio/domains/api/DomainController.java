package com.amg.digitalitzacio.domains.api;

import com.amg.digitalitzacio.domains.api.dto.*;
import com.amg.digitalitzacio.domains.application.DomainService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Controlador REST per al mòdul de dominis (Mòdul 23 - Domain Reseller)
@RestController
@RequestMapping("/api/v1/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;

    // Llista tots els dominis del sistema (paginat) — només SUPER_ADMIN
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<ManagedDomainResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return domainService.listAll(page, size);
    }

    // Llista dominis d'un tenent concret — ADMIN o SUPER_ADMIN
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<ManagedDomainResponse> listByTenant(@PathVariable UUID tenantId) {
        return domainService.listByTenant(tenantId);
    }

    // Llista els propis dominis del client autenticat
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public List<ManagedDomainResponse> listMy(@AuthenticationPrincipal UserPrincipal principal) {
        return domainService.listByTenant(principal.tenantId());
    }

    // Obté el detall d'un domini per ID — ADMIN o SUPER_ADMIN
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ManagedDomainResponse getDomain(@PathVariable UUID id) {
        return domainService.getDomain(id);
    }

    // Consulta la disponibilitat d'un domini i el preu de venda — ADMIN o SUPER_ADMIN
    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public DomainCheckResponse checkAvailability(@RequestBody @Valid DomainCheckRequest request) {
        return domainService.checkAvailability(request.domainName());
    }

    // Registra un nou domini — ADMIN o SUPER_ADMIN
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ManagedDomainResponse registerDomain(@RequestBody @Valid RegisterDomainRequest request) {
        return domainService.registerDomain(request);
    }

    // Renova un domini per un any addicional — ADMIN o SUPER_ADMIN
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ManagedDomainResponse renewDomain(@PathVariable UUID id) {
        return domainService.renewDomain(id);
    }

    // Configura el DNS del domini apuntant al servidor principal — ADMIN o SUPER_ADMIN
    @PostMapping("/{id}/configure-dns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ManagedDomainResponse configureDns(@PathVariable UUID id) {
        return domainService.configureDns(id, null);
    }

    // Actualitza els registres DNS d'un domini — ADMIN o SUPER_ADMIN
    @PutMapping("/{id}/dns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ManagedDomainResponse updateDnsRecords(
            @PathVariable UUID id,
            @RequestBody @Valid List<DnsRecordRequest> records) {
        return domainService.updateDnsRecords(id, records);
    }

    // Cancel·la un domini — només SUPER_ADMIN
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void cancelDomain(@PathVariable UUID id) {
        domainService.cancelDomain(id);
    }

    // Llista les tarifes de tots els TLDs actius — només SUPER_ADMIN
    @GetMapping("/admin/tld-pricing")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<TldPricingResponse> listTldPricing() {
        return domainService.listTldPricing();
    }

    // Actualitza o crea les tarifes d'un TLD — només SUPER_ADMIN
    @PutMapping("/admin/tld-pricing/{tld}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TldPricingResponse updateTldPricing(
            @PathVariable String tld,
            @RequestBody @Valid TldPricingRequest request) {
        return domainService.updateTldPricing(tld, request);
    }

    // Llista dominis que caduquen en els propers N dies — només SUPER_ADMIN
    @GetMapping("/admin/expiring")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<ManagedDomainResponse> listExpiring(
            @RequestParam(defaultValue = "30") int days) {
        return domainService.listExpiring(days);
    }
}
