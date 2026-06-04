package com.amg.digitalitzacio.leads.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Lead> findByTenantIdAndStage(UUID tenantId, PipelineStage stage, Pageable pageable);

    Page<Lead> findByTenantIdAndSource(UUID tenantId, LeadSource source, Pageable pageable);

    Page<Lead> findByTenantIdAndAssignedTo(UUID tenantId, UUID assignedTo, Pageable pageable);

    Page<Lead> findByTenantIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
            UUID tenantId, String name, String email, String phone, Pageable pageable);

    // SUPER_ADMIN queries (no tenant filter)
    Page<Lead> findByStage(PipelineStage stage, Pageable pageable);

    Page<Lead> findBySource(LeadSource source, Pageable pageable);

    Page<Lead> findByAssignedTo(UUID assignedTo, Pageable pageable);

    Page<Lead> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
            String name, String email, String phone, Pageable pageable);

    List<Lead> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    java.util.Optional<Lead> findFirstByTenantIdAndPhone(UUID tenantId, String phone);

    java.util.Optional<Lead> findFirstByTenantIdAndEmail(UUID tenantId, String email);

    void deleteByTenantId(UUID tenantId);

    List<Lead> findByUpdatedAtBeforeAndIsActive(Instant cutoff, Boolean isActive);

    List<Lead> findByUpdatedAtBefore(Instant cutoff);

    // Counts
    long countByTenantId(UUID tenantId);

    long countByTenantIdAndCreatedAtAfter(UUID tenantId, Instant since);

    long countByTenantIdAndCreatedAtAfterAndCreatedAtBefore(UUID tenantId, Instant after, Instant before);

    long countByTenantIdAndStage(UUID tenantId, PipelineStage stage);

    long countByTenantIdAndSource(UUID tenantId, LeadSource source);

    long countByStage(PipelineStage stage);

    long countBySource(LeadSource source);
}
