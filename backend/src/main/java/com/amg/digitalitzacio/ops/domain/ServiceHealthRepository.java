package com.amg.digitalitzacio.ops.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceHealthRepository extends JpaRepository<ServiceHealth, UUID> {
    Optional<ServiceHealth> findTopByServiceNameOrderByCheckedAtDesc(String serviceName);

    List<ServiceHealth> findTopByServiceNameOrderByCheckedAtDesc(String serviceName, Pageable pageable);

    default List<ServiceHealth> findTopByServiceNameOrderByCheckedAtDesc(String serviceName, int limit) {
        return findTopByServiceNameOrderByCheckedAtDesc(serviceName, Pageable.ofSize(limit));
    }
}
