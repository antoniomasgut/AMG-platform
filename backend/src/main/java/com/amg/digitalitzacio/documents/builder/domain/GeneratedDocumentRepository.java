package com.amg.digitalitzacio.documents.builder.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID> {
    List<GeneratedDocument> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<GeneratedDocument> findByTenantIdAndStatus(UUID tenantId, DocumentStatus status);
    long countByTenantId(UUID tenantId);
}
