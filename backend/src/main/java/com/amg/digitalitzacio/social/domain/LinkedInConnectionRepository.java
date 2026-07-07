package com.amg.digitalitzacio.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LinkedInConnectionRepository extends JpaRepository<LinkedInConnection, UUID> {

    Optional<LinkedInConnection> findByTenantId(UUID tenantId);
}
