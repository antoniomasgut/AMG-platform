package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.agents.domain.KnowledgeBaseRepository;
import com.amg.digitalitzacio.agents.domain.KnowledgeEntryRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.application.ClientUserProvisioningService;
import com.amg.digitalitzacio.auth.application.PhaseActivationService;
import com.amg.digitalitzacio.auth.application.PhaseHealthService;
import com.amg.digitalitzacio.auth.application.TenantService;
import com.amg.digitalitzacio.auth.domain.TenantPhaseActivation;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.application.PostAcceptanceService;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.engine.domain.LandingStatus;
import com.amg.digitalitzacio.hosting.domain.WebSiteRepository;
import com.amg.digitalitzacio.hosting.domain.WebsiteStatus;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final PhaseActivationService phaseActivationService;
    private final PhaseHealthService phaseHealthService;
    private final TenantRepository tenantRepository;
    private final PostAcceptanceService postAcceptanceService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final BudgetSetupIntakeRepository intakeRepository;
    private final WebSiteRepository webSiteRepository;
    private final LandingRepository landingRepository;
    private final ClientUserProvisioningService clientUserProvisioningService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        var response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<TenantResponse>> listTenants(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isFree) {
        // Native query with explicit ORDER BY — force unsorted pageable to avoid camelCase column translation
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        return ResponseEntity.ok(tenantService.listTenants(unsorted, search, isActive, isFree));
    }

    @GetMapping("/me/features")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getMyFeatures(@AuthenticationPrincipal UserPrincipal principal) {
        var tenantId = principal.tenantId();
        if (tenantId == null) {
            return ResponseEntity.ok(Map.of("contractedPhases", List.of()));
        }
        var tenant = tenantRepository.findById(tenantId).orElseThrow();
        // Fases actives: només les que SUPER_ADMIN ha posat en marxa explícitament (activePhases).
        // contractedPhases = pagat però pendent de configuració → no habilita funcions de l'agent.
        String activePhases = tenant.getActivePhases();
        List<String> phaseList = (activePhases != null && !activePhases.isBlank())
                ? Arrays.asList(activePhases.split(","))
                : List.of();
        // contractedPhases s'exposa per mostrar a la UI el que el client té pagat
        List<String> contractedList = (tenant.getContractedPhases() != null && !tenant.getContractedPhases().isBlank())
                ? Arrays.asList(tenant.getContractedPhases().split(","))
                : List.of();
        boolean hasF1 = phaseList.contains("F1");
        boolean hasF2 = phaseList.contains("F2");
        boolean hasF3 = phaseList.contains("F3");
        boolean hasF4 = phaseList.contains("F4");
        boolean hasF5 = phaseList.contains("F5");

        var features = new HashMap<String, Object>();
        features.put("contractedPhases",   contractedList);
        features.put("activePhases",       phaseList);
        features.put("canAccessAgent",     hasF1);
        features.put("canAccessAgenda",    hasF2);
        features.put("canAccessBilling",   hasF3);
        features.put("canAccessLeads",     hasF3 || hasF4);
        features.put("canAccessStorage",   hasF5);
        features.put("canAccessDocuments", hasF5);
        features.put("hasF1", hasF1);
        features.put("hasF2", hasF2);
        features.put("hasF3", hasF3);
        features.put("hasF4", hasF4);
        features.put("hasF5", hasF5);
        return ResponseEntity.ok(features);
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResponse> getOwnerTenant() {
        Optional<TenantResponse> owner = tenantService.getOwnerTenant();
        if (owner.isPresent()) return ResponseEntity.ok(owner.get());
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and authentication.principal.tenantId == #id)")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.getTenant(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @GetMapping("/{id}/delete-check")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantService.DeleteTenantCheckResponse> checkDeletion(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.checkDeletion(id));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantResponse> markImplementationDelivered(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.markImplementationDelivered(id));
    }

    @PostMapping("/{id}/onboarding-complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantResponse> markOnboardingCompleted(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.markOnboardingCompleted(id));
    }

    @PutMapping("/{id}/billing-start-date")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantResponse> setBillingStartDate(@PathVariable UUID id,
                                                              @Valid @RequestBody SetBillingDateRequest request) {
        return ResponseEntity.ok(tenantService.setBillingStartDate(id, request.billingStartDate()));
    }

    record ReadinessResponse(
            boolean intakeCompleted,
            boolean hasLanding,
            boolean hasKbEntries,
            boolean hasChannel,
            boolean ready
    ) {}

    @GetMapping("/{id}/readiness")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ReadinessResponse> getReadiness(@PathVariable UUID id) {
        boolean intakeCompleted = intakeRepository.existsByTenantIdAndStatus(id, "COMPLETE");

        // Landing del motor (producte estàndard, mòduls 04/05) o web allotjada/importada (mòdul 29)
        var websites = webSiteRepository.findByTenantId(id);
        boolean hasLanding = landingRepository.countByTenantIdAndStatus(id, LandingStatus.PUBLISHED) > 0
                || websites.stream().anyMatch(w -> w.getStatus() == WebsiteStatus.ACTIVE);

        boolean hasKbEntries = knowledgeBaseRepository.findByTenantId(id)
                .map(kb -> knowledgeEntryRepository.countByKnowledgeBaseId(kb.getId()) > 0)
                .orElse(false);

        var chatLink = chatLinkRepository.findByTenantId(id).orElse(null);
        boolean hasChannel = chatLink != null && (
                Boolean.TRUE.equals(chatLink.getWhatsappEnabled()) ||
                Boolean.TRUE.equals(chatLink.getEmailEnabled()) ||
                Boolean.TRUE.equals(chatLink.getWidgetEnabled()) ||
                chatLink.getTelegramChatId() != null
        );

        boolean ready = intakeCompleted && hasLanding && hasKbEntries && hasChannel;
        return ResponseEntity.ok(new ReadinessResponse(intakeCompleted, hasLanding, hasKbEntries, hasChannel, ready));
    }

    @PostMapping("/{id}/go-live")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void goLive(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tenant not found: " + id));
        if (tenant.getContractedPhases() == null || tenant.getContractedPhases().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                    "El tenant no té fases contractades — assigna-les abans del go-live");
        }
        tenant.setActivePhases(tenant.getContractedPhases());
        if (tenant.getBillingStartDate() == null && !Boolean.TRUE.equals(tenant.getIsFree())) {
            tenant.setBillingStartDate(java.time.LocalDate.now());
        }
        tenantRepository.save(tenant);
        // Historial de fases (idempotent si ja constaven actives des del pagament)
        for (String phase : tenant.getContractedPhases().split(",")) {
            phaseActivationService.recordActivation(id, phase.trim(), "GO_LIVE", null,
                    principal != null ? principal.email() : null);
        }
        // Usuari CLIENT + invitació per establir contrasenya (si encara no en té)
        clientUserProvisioningService.provisionForTenant(id);
        postAcceptanceService.onGoLive(id);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tenant not found: " + id));
        // Registrar desactivació de les fases operatives (activePhases null = totes les contractades)
        String effective = tenant.getActivePhases() != null ? tenant.getActivePhases() : tenant.getContractedPhases();
        if (effective != null && !effective.isBlank()) {
            for (String phase : effective.split(",")) {
                phaseActivationService.recordDeactivation(id, phase.trim(),
                        principal != null ? principal.email() : null);
            }
        }
        tenant.setActivePhases("");
        tenantRepository.save(tenant);
    }

    @GetMapping("/{id}/phase-history")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<TenantPhaseActivation>> getPhaseHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(phaseActivationService.getHistory(id));
    }

    @GetMapping("/{id}/phase-health")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantPhaseHealthResponse> getPhaseHealth(@PathVariable UUID id) {
        return ResponseEntity.ok(phaseHealthService.checkHealth(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
    }
}
