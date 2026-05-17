package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CredentialFieldRepository extends JpaRepository<CredentialField, UUID> {
    List<CredentialField> findByServiceIdOrderBySortOrder(UUID serviceId);
    List<CredentialField> findByServiceIdIn(List<UUID> serviceIds);
    java.util.Optional<CredentialField> findByServiceIdAndKey(UUID serviceId, String key);
}
