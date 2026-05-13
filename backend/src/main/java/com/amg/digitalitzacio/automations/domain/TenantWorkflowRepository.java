package com.amg.digitalitzacio.automations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantWorkflowRepository extends JpaRepository<TenantWorkflow, UUID> {
    Page<TenantWorkflow> findByTenantId(UUID tenantId, Pageable pageable);
    Page<TenantWorkflow> findByTenantIdAndStatus(UUID tenantId, TenantWorkflowStatus status, Pageable pageable);
    Optional<TenantWorkflow> findByTenantIdAndTemplateIdAndIsActiveTrue(UUID tenantId, UUID templateId);
    Optional<TenantWorkflow> findByIdAndIsActiveTrue(UUID id);
    long countByTenantIdAndStatus(UUID tenantId, TenantWorkflowStatus status);
}
