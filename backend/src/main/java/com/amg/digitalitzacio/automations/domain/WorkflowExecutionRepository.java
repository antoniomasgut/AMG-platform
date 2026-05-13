package com.amg.digitalitzacio.automations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {
    Page<WorkflowExecution> findByTenantWorkflowId(UUID tenantWorkflowId, Pageable pageable);
    Optional<WorkflowExecution> findByN8nExecutionId(String executionId);
}
