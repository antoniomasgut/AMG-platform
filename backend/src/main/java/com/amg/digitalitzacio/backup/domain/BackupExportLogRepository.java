package com.amg.digitalitzacio.backup.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BackupExportLogRepository extends JpaRepository<BackupExportLog, UUID> {

    List<BackupExportLog> findByTaskIdOrderByStartedAt(UUID taskId);
}
