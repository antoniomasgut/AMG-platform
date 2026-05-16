package com.amg.digitalitzacio.backup.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestoreTaskRepository extends JpaRepository<RestoreTask, UUID> {

    List<RestoreTask> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
