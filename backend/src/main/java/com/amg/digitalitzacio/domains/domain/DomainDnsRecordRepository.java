package com.amg.digitalitzacio.domains.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Repositori JPA per a registres DNS dels dominis
public interface DomainDnsRecordRepository extends JpaRepository<DomainDnsRecord, UUID> {

    List<DomainDnsRecord> findByDomainId(UUID domainId);

    @Modifying
    @Transactional
    void deleteByDomainId(UUID domainId);
}
