package com.amg.digitalitzacio.engine.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactLeadRepository extends JpaRepository<ContactLead, UUID> {
    Page<ContactLead> findByLandingId(UUID landingId, Pageable pageable);
    List<ContactLead> findByLandingId(UUID landingId);
    long countByLandingId(UUID landingId);
}
