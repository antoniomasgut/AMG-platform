package com.amg.digitalitzacio.engine.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LandingTemplateRepository extends JpaRepository<LandingTemplate, UUID> {
    List<LandingTemplate> findByIsActiveTrue();
    Optional<LandingTemplate> findBySlug(String slug);
}
