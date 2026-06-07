package com.amg.digitalitzacio.ops.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findByStatusOrderByStartedAtDesc(IncidentStatus status);
    List<Incident> findByStatusAndServiceName(IncidentStatus status, String serviceName);
    List<Incident> findByServiceNameAndStatus(String serviceName, IncidentStatus status);
    Optional<Incident> findTopByServiceNameAndStatusOrderByResolvedAtDesc(String serviceName, IncidentStatus status);
}
