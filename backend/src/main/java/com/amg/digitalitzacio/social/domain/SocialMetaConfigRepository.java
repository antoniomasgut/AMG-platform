package com.amg.digitalitzacio.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SocialMetaConfigRepository extends JpaRepository<SocialMetaConfig, UUID> {

    Optional<SocialMetaConfig> findByTenantId(UUID tenantId);
}
