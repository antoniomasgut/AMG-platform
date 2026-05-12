package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceProfileRepository extends JpaRepository<ServiceProfile, UUID> {
    Optional<ServiceProfile> findBySlug(String slug);
    List<ServiceProfile> findByIsActiveTrue();
}
