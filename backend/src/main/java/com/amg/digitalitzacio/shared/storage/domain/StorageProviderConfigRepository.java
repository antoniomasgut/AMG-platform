package com.amg.digitalitzacio.shared.storage.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageProviderConfigRepository extends JpaRepository<StorageProviderConfig, StorageProviderConfig.PK> {
    List<StorageProviderConfig> findByTenantId(UUID tenantId);
    Optional<StorageProviderConfig> findByTenantIdAndActiveTrue(UUID tenantId);
    Optional<StorageProviderConfig> findByTenantIdAndProviderKey(UUID tenantId, String providerKey);
}
