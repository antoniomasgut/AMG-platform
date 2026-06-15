package com.amg.digitalitzacio.documents.delivery.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecureDocumentAuditRepository extends JpaRepository<SecureDocumentAudit, UUID> {

    List<SecureDocumentAudit> findByTokenIdOrderByOccurredAtDesc(UUID tokenId);
}
