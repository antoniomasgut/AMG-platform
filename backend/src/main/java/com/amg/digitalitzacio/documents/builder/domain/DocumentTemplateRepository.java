package com.amg.digitalitzacio.documents.builder.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID> {
    List<DocumentTemplate> findByTenantIdOrderByUpdatedAtDesc(UUID tenantId);
    List<DocumentTemplate> findByTenantIdAndActiveTrue(UUID tenantId);
}
