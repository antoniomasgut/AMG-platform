package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemoSessionRepository extends JpaRepository<DemoSession, UUID> {

    Optional<DemoSession> findByToken(UUID token);

    // Neteja de sessions expirades (ús en scheduler o tasques de manteniment)
    void deleteByExpiresAtBefore(Instant threshold);
}
