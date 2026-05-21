package com.amg.digitalitzacio.domains.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repositori JPA per a dominis gestionats
public interface ManagedDomainRepository extends JpaRepository<ManagedDomain, UUID> {

    List<ManagedDomain> findByTenantId(UUID tenantId);

    Optional<ManagedDomain> findByDomainName(String domainName);

    List<ManagedDomain> findByStatusIn(List<DomainStatus> statuses);

    // Dominis que caduquen aviat i tenen renovació automàtica activa
    List<ManagedDomain> findByExpiresAtBeforeAndAutoRenewTrue(Instant date);

    boolean existsByDomainName(String domainName);
}
