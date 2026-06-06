package com.amg.digitalitzacio.google.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthStateRepository extends JpaRepository<OAuthState, UUID> {
    Optional<OAuthState> findByStateToken(String stateToken);
    void deleteByTenantId(UUID tenantId);
}
