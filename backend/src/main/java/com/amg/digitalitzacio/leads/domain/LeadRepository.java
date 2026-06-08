package com.amg.digitalitzacio.leads.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.isActive = true")
    Page<Lead> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.stage = :stage AND l.isActive = true")
    Page<Lead> findByTenantIdAndStage(@Param("tenantId") UUID tenantId, @Param("stage") PipelineStage stage, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.source = :source AND l.isActive = true")
    Page<Lead> findByTenantIdAndSource(@Param("tenantId") UUID tenantId, @Param("source") LeadSource source, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.assignedTo = :assignedTo AND l.isActive = true")
    Page<Lead> findByTenantIdAndAssignedTo(@Param("tenantId") UUID tenantId, @Param("assignedTo") UUID assignedTo, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.isActive = true " +
           "AND (LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(l.email) LIKE LOWER(CONCAT('%', :email, '%')) " +
           "OR l.phone LIKE CONCAT('%', :phone, '%'))")
    Page<Lead> findByTenantIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
            @Param("tenantId") UUID tenantId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            Pageable pageable);

    // SUPER_ADMIN queries (no tenant filter)
    @Query("SELECT l FROM Lead l WHERE l.stage = :stage AND l.isActive = true")
    Page<Lead> findByStage(@Param("stage") PipelineStage stage, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.source = :source AND l.isActive = true")
    Page<Lead> findBySource(@Param("source") LeadSource source, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.assignedTo = :assignedTo AND l.isActive = true")
    Page<Lead> findByAssignedTo(@Param("assignedTo") UUID assignedTo, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.isActive = true AND " +
           "(LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(l.email) LIKE LOWER(CONCAT('%', :email, '%')) " +
           "OR l.phone LIKE CONCAT('%', :phone, '%'))")
    Page<Lead> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.isActive = true")
    Page<Lead> findAllActive(Pageable pageable);

    List<Lead> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    boolean existsByTenantIdAndMetaLeadId(UUID tenantId, String metaLeadId);

    java.util.Optional<Lead> findFirstByTenantIdAndPhone(UUID tenantId, String phone);

    java.util.Optional<Lead> findFirstByTenantIdAndEmail(UUID tenantId, String email);

    void deleteByTenantId(UUID tenantId);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.isActive = true AND l.stage IN :stages " +
           "AND (l.lastContactAt IS NULL OR l.lastContactAt < :cutoff) AND l.updatedAt < :cutoff")
    List<Lead> findStaleByTenantIdAndStages(@Param("tenantId") UUID tenantId,
                                            @Param("stages") List<PipelineStage> stages,
                                            @Param("cutoff") Instant cutoff);

    List<Lead> findByUpdatedAtBeforeAndIsActive(Instant cutoff, Boolean isActive);

    List<Lead> findByUpdatedAtBefore(Instant cutoff);

    // Counts
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.isActive = true")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.isActive = true")
    long countActive();

    long countByTenantIdAndCreatedAtAfter(UUID tenantId, Instant since);

    long countByTenantIdAndCreatedAtAfterAndCreatedAtBefore(UUID tenantId, Instant after, Instant before);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.stage = :stage AND l.isActive = true")
    long countByTenantIdAndStage(@Param("tenantId") UUID tenantId, @Param("stage") PipelineStage stage);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.source = :source AND l.isActive = true")
    long countByTenantIdAndSource(@Param("tenantId") UUID tenantId, @Param("source") LeadSource source);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.stage = :stage AND l.isActive = true")
    long countByStage(@Param("stage") PipelineStage stage);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.source = :source AND l.isActive = true")
    long countBySource(@Param("source") LeadSource source);

    @Query("SELECT l.utmCampaign, COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.utmCampaign IS NOT NULL GROUP BY l.utmCampaign")
    List<Object[]> countByUtmCampaign(UUID tenantId);

    @Modifying
    @Query("UPDATE Lead l SET l.isActive = false WHERE l.createdAt < :cutoff AND l.isActive = true")
    int softDeleteLeadsOlderThan(@Param("cutoff") Instant cutoff);
}
