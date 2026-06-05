package com.amg.digitalitzacio.metaads.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdCreativeRepository extends JpaRepository<AdCreative, UUID> {
    List<AdCreative> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<AdCreative> findByIdAndTenantId(UUID id, UUID tenantId);
}
