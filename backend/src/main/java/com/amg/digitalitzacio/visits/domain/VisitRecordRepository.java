package com.amg.digitalitzacio.visits.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, UUID> {

    List<VisitRecord> findByTenantIdAndContactIdentifierOrderByVisitDateDesc(
            UUID tenantId, String contactIdentifier);

    List<VisitRecord> findByTenantIdOrderByVisitDateDesc(UUID tenantId);

    // Clients amb revisió vençuda (per al recordatori de F4)
    List<VisitRecord> findByTenantIdAndNextVisitDueBefore(UUID tenantId, LocalDate date);

    long countByTenantId(UUID tenantId);

    void deleteByTenantId(UUID tenantId);
}
