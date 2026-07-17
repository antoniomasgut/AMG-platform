package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CatalogServiceRepository extends JpaRepository<CatalogService, UUID> {
    List<CatalogService> findByPhaseIdOrderBySortOrder(UUID phaseId);
    List<CatalogService> findByPhaseIdIsNullOrderByNameAsc();
    List<CatalogService> findByProfileIdAndPhaseIdIsNull(UUID profileId);
    List<CatalogService> findByIsAddonTrue();
    List<CatalogService> findByType(ServiceType type);
    java.util.Optional<CatalogService> findBySlug(String slug);
    java.util.Optional<CatalogService> findBySlugAndPhaseIdIsNull(String slug);
    boolean existsBySlug(String slug);
}
