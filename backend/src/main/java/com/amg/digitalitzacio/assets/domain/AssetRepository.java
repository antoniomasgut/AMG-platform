package com.amg.digitalitzacio.assets.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    Optional<Asset> findByIdAndIsActiveTrue(UUID id);

    long countByTenantIdAndIsActiveTrue(UUID tenantId);
}
