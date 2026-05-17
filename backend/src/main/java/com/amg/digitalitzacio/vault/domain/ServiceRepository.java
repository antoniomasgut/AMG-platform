package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<CatalogService, UUID> {
    List<CatalogService> findByPhaseIdOrderBySortOrder(UUID phaseId);
    List<CatalogService> findByIsAddonTrue();
}
