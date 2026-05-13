package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantCredentialRepository extends JpaRepository<TenantCredential, UUID> {
    Optional<TenantCredential> findByTenantIdAndFieldId(UUID tenantId, UUID fieldId);
    List<TenantCredential> findByTenantId(UUID tenantId);
}
