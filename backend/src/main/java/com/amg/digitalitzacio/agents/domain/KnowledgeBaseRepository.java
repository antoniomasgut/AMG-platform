package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {
    Optional<KnowledgeBase> findByTenantId(UUID tenantId);
    void deleteByTenantId(UUID tenantId);
}
