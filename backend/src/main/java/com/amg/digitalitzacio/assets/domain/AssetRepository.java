package com.amg.digitalitzacio.assets.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    Optional<Asset> findByIdAndIsActiveTrue(UUID id);

    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    @Query("SELECT COALESCE(SUM(a.size), 0) FROM Asset a WHERE a.tenantId = :tenantId AND a.isActive = true")
    long sumSizeByTenantId(UUID tenantId);
}
