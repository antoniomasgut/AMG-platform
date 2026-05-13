package com.amg.digitalitzacio.engine.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LandingVersionRepository extends JpaRepository<LandingVersion, UUID> {
    List<LandingVersion> findByLandingIdOrderByVersionNumberDesc(UUID landingId);
    Optional<LandingVersion> findByLandingIdAndId(UUID landingId, UUID id);
    Optional<LandingVersion> findTopByLandingIdAndStatusOrderByVersionNumberDesc(UUID landingId, VersionStatus status);
    long countByLandingId(UUID landingId);
}
