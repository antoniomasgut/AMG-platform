package com.amg.digitalitzacio.shared.sysconfig.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SystemConfigAuditLogRepository extends JpaRepository<SystemConfigAuditLog, UUID> {
    List<SystemConfigAuditLog> findByConfigKeyOrderByChangedAtDesc(String configKey);
}
