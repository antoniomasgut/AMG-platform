package com.amg.digitalitzacio.documents.builder.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentTemplateVersionRepository extends JpaRepository<DocumentTemplateVersion, UUID> {
    List<DocumentTemplateVersion> findByTemplateIdOrderByVersionDesc(UUID templateId);
    Optional<DocumentTemplateVersion> findByTemplateIdAndVersion(UUID templateId, Integer version);
}
