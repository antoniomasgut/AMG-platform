package com.amg.digitalitzacio.ops.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BackupRecordRepository extends JpaRepository<BackupRecord, UUID> {
    List<BackupRecord> findByTypeOrderByStartedAtDesc(BackupType type);
}
