package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NexeServiceConfigRepository extends JpaRepository<NexeServiceConfig, NexeServiceConfig.PK> {

    List<NexeServiceConfig> findByTenantId(UUID tenantId);

    Optional<NexeServiceConfig> findByTenantIdAndServiceKey(UUID tenantId, String serviceKey);
}
