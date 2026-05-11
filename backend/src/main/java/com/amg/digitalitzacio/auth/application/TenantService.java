package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse createTenant(@Valid CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug ja existeix");
        }

        var tenant = Tenant.builder()
                .name(request.name())
                .slug(request.slug())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .isActive(true)
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

        tenant = tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(), tenant.getName(), tenant.getSlug(),
                tenant.getEmail(), tenant.getPhone(), tenant.getAddress(),
                tenant.getIsActive(), tenant.getCreatedAt());
    }
}
