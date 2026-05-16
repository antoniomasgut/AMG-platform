package com.amg.digitalitzacio.backup.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackupTaskRepository extends JpaRepository<BackupTask, UUID> {

    Page<BackupTask> findAllByOrderByStartedAtDesc(Pageable pageable);

    Optional<BackupTask> findTopByOrderByStartedAtDesc();

    List<BackupTask> findByRetentionUntilBefore(Instant now);
}
