package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    List<Contact> findByTenantId(UUID tenantId);
    Optional<Contact> findByTenantIdAndPhone(UUID tenantId, String phone);
    Optional<Contact> findByTenantIdAndEmail(UUID tenantId, String email);
}
