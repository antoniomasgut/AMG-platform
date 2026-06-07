package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.agents.application.SectorKnowledgeSeedService;
import com.amg.digitalitzacio.agents.domain.KnowledgeBaseRepository;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import com.amg.digitalitzacio.auth.domain.BusinessSize;
import com.amg.digitalitzacio.auth.domain.PreferredChannel;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.finops.domain.MonthlyInvoiceRepository;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessConfigRepository;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessMandateRepository;
import com.amg.digitalitzacio.vault.domain.TenantProfileRepository;
import com.amg.digitalitzacio.vault.domain.TenantServiceRepository;
import com.amg.digitalitzacio.telegram.domain.TenantTelegramConfigRepository;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantServiceRepository tenantServiceRepository;
    private final TenantProfileRepository tenantProfileRepository;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final TenantAIConfigRepository tenantAIConfigRepository;
    private final BudgetRepository budgetRepository;
    private final MonthlyInvoiceRepository monthlyInvoiceRepository;
    private final GoCardlessConfigRepository goCardlessConfigRepository;
    private final GoCardlessMandateRepository goCardlessMandateRepository;
    private final WhatsAppWabaConfigRepository whatsAppWabaConfigRepository;
    private final TenantTelegramConfigRepository tenantTelegramConfigRepository;
    private final SectorKnowledgeSeedService sectorKnowledgeSeedService;

    @Transactional
    public TenantResponse createTenant(@Valid CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug ja existeix");
        }

        BusinessSector sector = request.sector() != null ? BusinessSector.valueOf(request.sector().toUpperCase()) : null;

        var tenant = Tenant.builder()
                .name(request.name())
                .slug(request.slug())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .nif(request.nif())
                .contactPhone(request.contactPhone())
                .preferredChannel(request.preferredChannel() != null
                        ? PreferredChannel.valueOf(request.preferredChannel().toUpperCase())
                        : null)
                .sector(sector)
                .businessSize(request.businessSize() != null ? BusinessSize.valueOf(request.businessSize().toUpperCase()) : null)
                .contractedPhases(toPhaseString(request.contractedPhases()))
                .agentSystemPrompt(resolveAgentPrompt(request.agentSystemPrompt(), sector))
                .isActive(true)
                .isFree(request.isFree() != null && request.isFree())
                .build();

        tenant = tenantRepository.save(tenant);
        sectorKnowledgeSeedService.seed(tenant.getId(), sector, tenant.getName(), tenant.getCity());
        // Auto-set billing start date if has contracted phases and is not free
        if (request.contractedPhases() != null && !request.contractedPhases().isEmpty()
                && !Boolean.TRUE.equals(request.isFree())) {
            tenant.setBillingStartDate(LocalDate.now());
            tenant = tenantRepository.save(tenant);
        }
        return toResponse(tenant);
    }

    public Page<TenantResponse> listTenants(Pageable pageable, String search, Boolean isActive, Boolean isFree) {
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        return tenantRepository.findFiltered(searchParam, isActive, isFree, pageable).map(this::toResponse);
    }

    public TenantResponse getTenant(UUID id) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no trobat"));
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(UUID id, @Valid UpdateTenantRequest request) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no trobat"));

        if (request.name() != null) tenant.setName(request.name());
        if (request.slug() != null) {
            if (!request.slug().equals(tenant.getSlug()) && tenantRepository.existsBySlug(request.slug())) {
                throw new IllegalArgumentException("Slug ja existeix");
            }
            tenant.setSlug(request.slug());
        }
        if (request.email() != null) tenant.setEmail(request.email());
        if (request.phone() != null) tenant.setPhone(request.phone());
        if (request.address() != null) tenant.setAddress(request.address());
        if (request.city() != null) tenant.setCity(request.city());
        if (request.nif() != null) tenant.setNif(request.nif());
        if (request.contactPhone() != null) tenant.setContactPhone(request.contactPhone());
        if (request.preferredChannel() != null) tenant.setPreferredChannel(
                PreferredChannel.valueOf(request.preferredChannel().toUpperCase()));
        if (request.sector() != null) tenant.setSector(BusinessSector.valueOf(request.sector().toUpperCase()));
        if (request.businessSize() != null) tenant.setBusinessSize(BusinessSize.valueOf(request.businessSize().toUpperCase()));
        if (request.contractedPhases() != null) tenant.setContractedPhases(toPhaseString(request.contractedPhases()));
        if (request.activePhases() != null) tenant.setActivePhases(toPhaseString(request.activePhases()));
        if (request.agentSystemPrompt() != null) tenant.setAgentSystemPrompt(request.agentSystemPrompt());
        if (request.isFree() != null) tenant.setIsFree(request.isFree());
        if (request.isActive() != null) tenant.setIsActive(request.isActive());

        tenant = tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse markImplementationDelivered(UUID id) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no trobat"));
        
        tenant.setImplementationDeliveredAt(Instant.now());
        if (tenant.getBillingStartDate() == null) {
            tenant.setBillingStartDate(LocalDate.now());
        }
        
        tenant = tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse markOnboardingCompleted(UUID id) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no trobat"));
        
        tenant.setOnboardingCompletedAt(Instant.now());
        
        tenant = tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse setBillingStartDate(UUID id, LocalDate date) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no trobat"));
        
        tenant.setBillingStartDate(date);
        
        tenant = tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    private String toPhaseString(List<String> phases) {
        if (phases == null || phases.isEmpty()) return null;
        return phases.stream()
                .map(String::toUpperCase)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private List<String> fromPhaseString(String phases) {
        if (phases == null || phases.isBlank()) return null;
        return Arrays.asList(phases.split(","));
    }

    private String resolveAgentPrompt(String provided, BusinessSector sector) {
        if (provided != null && !provided.isBlank()) return provided;
        if (sector != null) return SectorPromptTemplates.getTemplate(sector);
        return null;
    }

    /** Comprova si un tenant es pot eliminar. Només les factures bloquegen l'eliminació. */
    @Transactional(readOnly = true)
    public DeleteTenantCheckResponse checkDeletion(UUID id) {
        tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no trobat"));

        List<String> warnings = new ArrayList<>();

        long invoices = monthlyInvoiceRepository.countByTenantId(id);
        if (invoices > 0) warnings.add(invoices + " factura" + (invoices > 1 ? "es" : "") +
                " quedaran com a registre històric (no s'eliminaran)");

        long activeServices = tenantServiceRepository.countByTenantIdAndIsEnabled(id, true);
        long disabledServices = tenantServiceRepository.countByTenantIdAndIsEnabled(id, false);
        long totalServices = activeServices + disabledServices;
        if (totalServices > 0) warnings.add(totalServices + " servei" + (totalServices > 1 ? "s" : "") +
                " (" + activeServices + " actiu" + (activeServices != 1 ? "s" : "") + ") s'eliminaran");

        long budgets = budgetRepository.countByTenantId(id);
        if (budgets > 0) warnings.add(budgets + " pressupost" + (budgets > 1 ? "os" : "") + " s'eliminaran");

        var chatLink = tenantChatLinkRepository.findByTenantId(id);
        if (chatLink.isPresent()) warnings.add("La configuració de l'agent IA s'eliminarà");

        var kb = knowledgeBaseRepository.findByTenantId(id);
        if (kb.isPresent()) warnings.add("La base de coneixement s'eliminarà");

        long gcConfigs = goCardlessConfigRepository.countByTenantId(id);
        if (gcConfigs > 0) warnings.add("La configuració de GoCardless s'eliminarà");

        long wabaConfigs = whatsAppWabaConfigRepository.countByTenantId(id);
        if (wabaConfigs > 0) warnings.add("La configuració de WhatsApp Business s'eliminarà");

        long tgConfigs = tenantTelegramConfigRepository.countByTenantId(id);
        if (tgConfigs > 0) warnings.add("La configuració del bot de Telegram s'eliminarà");

        return new DeleteTenantCheckResponse(true, List.of(), warnings);
    }

    /** Elimina un tenant i totes les seves dades associades (serveis, agent, KB, pagament). */
    @Transactional
    public void deleteTenant(UUID id) {
        tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no trobat"));

        // Re-validates before deleting
        var check = checkDeletion(id);
        if (!check.canDelete()) {
            throw new IllegalStateException("No es pot eliminar: " + String.join(", ", check.blockers()));
        }

        tenantServiceRepository.deleteByTenantId(id);
        tenantProfileRepository.deleteByTenantId(id);
        budgetRepository.deleteByTenantId(id);
        tenantChatLinkRepository.deleteByTenantId(id);
        knowledgeBaseRepository.deleteByTenantId(id);
        tenantAIConfigRepository.deleteById(id);
        goCardlessMandateRepository.deleteByTenantId(id);
        goCardlessConfigRepository.deleteByTenantId(id);
        whatsAppWabaConfigRepository.deleteByTenantId(id);
        tenantTelegramConfigRepository.deleteByTenantId(id);
        tenantRepository.deleteById(id);
    }

    /** DTO de resposta per a la comprovació prèvia a l'eliminació */
    public record DeleteTenantCheckResponse(boolean canDelete, List<String> blockers, List<String> warnings) {}

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(), tenant.getName(), tenant.getSlug(),
                tenant.getEmail(), tenant.getPhone(), tenant.getAddress(),
                tenant.getCity(), tenant.getNif(), tenant.getContactPhone(),
                tenant.getPreferredChannel() != null ? tenant.getPreferredChannel().name() : null,
                tenant.getSector() != null ? tenant.getSector().name() : null,
                tenant.getBusinessSize() != null ? tenant.getBusinessSize().name() : null,
                fromPhaseString(tenant.getContractedPhases()),
                fromPhaseString(tenant.getActivePhases()),
                tenant.getAgentSystemPrompt(),
                Boolean.TRUE.equals(tenant.getIsActive()), Boolean.TRUE.equals(tenant.getIsFree()),
                tenant.getBillingStartDate(),
                tenant.getImplementationDeliveredAt(),
                tenant.getOnboardingCompletedAt(),
                tenant.getCreatedAt());
    }
}
