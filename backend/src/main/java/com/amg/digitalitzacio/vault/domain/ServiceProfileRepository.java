package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceProfileRepository extends JpaRepository<ServiceProfile, UUID> {
    Optional<ServiceProfile> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
