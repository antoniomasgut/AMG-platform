package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import com.amg.digitalitzacio.auth.domain.BusinessSize;
import com.amg.digitalitzacio.auth.domain.PreferredChannel;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.validation.Valid;
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
        return toResponse(tenant);
    }

    public Page<TenantResponse> listTenants(Pageable pageable, String search) {
        return tenantRepository.findAll(pageable).map(this::toResponse);
    }

    public TenantResponse getTenant(UUID id) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no trobat"));
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(UUID id, @Valid UpdateTenantRequest request) {
        var tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no trobat"));

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
        if (request.nif() != null) tenant.setNif(request.nif());
        if (request.contactPhone() != null) tenant.setContactPhone(request.contactPhone());
        if (request.preferredChannel() != null) tenant.setPreferredChannel(
                PreferredChannel.valueOf(request.preferredChannel().toUpperCase()));
        if (request.sector() != null) tenant.setSector(BusinessSector.valueOf(request.sector().toUpperCase()));
        if (request.businessSize() != null) tenant.setBusinessSize(BusinessSize.valueOf(request.businessSize().toUpperCase()));
        if (request.contractedPhases() != null) tenant.setContractedPhases(toPhaseString(request.contractedPhases()));
        if (request.agentSystemPrompt() != null) tenant.setAgentSystemPrompt(request.agentSystemPrompt());
        if (request.isFree() != null) tenant.setIsFree(request.isFree());

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

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(), tenant.getName(), tenant.getSlug(),
                tenant.getEmail(), tenant.getPhone(), tenant.getAddress(),
                tenant.getNif(), tenant.getContactPhone(),
                tenant.getPreferredChannel() != null ? tenant.getPreferredChannel().name() : null,
                tenant.getSector() != null ? tenant.getSector().name() : null,
                tenant.getBusinessSize() != null ? tenant.getBusinessSize().name() : null,
                fromPhaseString(tenant.getContractedPhases()),
                tenant.getAgentSystemPrompt(),
                tenant.getIsActive(), Boolean.TRUE.equals(tenant.getIsFree()), tenant.getCreatedAt());
    }
}
