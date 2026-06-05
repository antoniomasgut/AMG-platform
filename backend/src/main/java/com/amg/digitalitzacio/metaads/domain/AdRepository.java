package com.amg.digitalitzacio.metaads.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdRepository extends JpaRepository<Ad, UUID> {
    List<Ad> findByAdSetIdOrderByCreatedAtAsc(UUID adSetId);
    Optional<Ad> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Ad> findByTenantId(UUID tenantId);
}
