package com.amg.digitalitzacio.automations.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {
    Optional<WorkflowTemplate> findByKey(String key);
    List<WorkflowTemplate> findByCategoryAndIsActiveTrue(WorkflowCategory category);
    List<WorkflowTemplate> findByIsActiveTrue();
}
