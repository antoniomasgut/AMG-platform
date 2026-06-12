package com.amg.digitalitzacio.legal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DpaSignatureRepository extends JpaRepository<DpaSignature, UUID> {
    Optional<DpaSignature> findByToken(String token);
    Optional<DpaSignature> findTopByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
