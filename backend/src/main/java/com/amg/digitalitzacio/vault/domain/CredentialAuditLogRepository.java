package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CredentialAuditLogRepository extends JpaRepository<CredentialAuditLog, UUID> {
    List<CredentialAuditLog> findByCredentialIdOrderByCreatedAtDesc(UUID credentialId);
}
