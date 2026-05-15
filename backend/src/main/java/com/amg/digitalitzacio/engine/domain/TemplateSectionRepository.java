package com.amg.digitalitzacio.engine.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TemplateSectionRepository extends JpaRepository<TemplateSection, UUID> {
    List<TemplateSection> findByTemplateIdOrderBySortOrder(UUID templateId);
    void deleteByTemplateId(UUID templateId);
}
