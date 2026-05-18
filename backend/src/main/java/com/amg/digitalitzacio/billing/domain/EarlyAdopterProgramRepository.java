package com.amg.digitalitzacio.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EarlyAdopterProgramRepository extends JpaRepository<EarlyAdopterProgram, UUID> {
    Optional<EarlyAdopterProgram> findFirstByOrderByUpdatedAtDesc();
}
