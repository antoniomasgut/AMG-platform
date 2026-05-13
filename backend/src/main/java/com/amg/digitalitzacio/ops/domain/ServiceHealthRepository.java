package com.amg.digitalitzacio.ops.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceHealthRepository extends JpaRepository<ServiceHealth, UUID> {
    Optional<ServiceHealth> findTopByServiceNameOrderByCheckedAtDesc(String serviceName);
}
