package com.amg.digitalitzacio.infraops.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InfraMetricSnapshotRepository extends JpaRepository<InfraMetricSnapshot, UUID> {
    Optional<InfraMetricSnapshot> findTopByOrderByCollectedAtDesc();
    Page<InfraMetricSnapshot> findByCollectedAtAfterOrderByCollectedAtDesc(Instant after, Pageable pageable);
    List<InfraMetricSnapshot> findByCollectedAtAfterOrderByCollectedAtDesc(Instant after);
    void deleteByCollectedAtBefore(Instant before);

    @Query("SELECT AVG(s.cpuPercent) FROM InfraMetricSnapshot s WHERE s.collectedAt > :after")
    Double avgCpuSince(Instant after);

    @Query("SELECT AVG(s.ramPercent) FROM InfraMetricSnapshot s WHERE s.collectedAt > :after")
    Double avgRamSince(Instant after);

    @Query("SELECT AVG(s.diskPercent) FROM InfraMetricSnapshot s WHERE s.collectedAt > :after")
    Double avgDiskSince(Instant after);

    @Query("SELECT AVG(s.dbActiveConnections * 100.0 / s.dbMaxConnections) FROM InfraMetricSnapshot s WHERE s.collectedAt > :after AND s.dbMaxConnections > 0")
    Double avgDbConnectionsPercentSince(Instant after);
}
