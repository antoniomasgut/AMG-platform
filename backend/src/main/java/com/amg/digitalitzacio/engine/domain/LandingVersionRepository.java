package com.amg.digitalitzacio.engine.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LandingVersionRepository extends JpaRepository<LandingVersion, UUID> {

    List<LandingVersion> findByLandingIdOrderByVersionNumberDesc(UUID landingId);

    List<LandingVersion> findByLandingIdAndLocaleOrderByVersionNumberDesc(UUID landingId, String locale);

    Optional<LandingVersion> findByLandingIdAndId(UUID landingId, UUID id);

    /** DRAFT més recent per locale — per publish */
    Optional<LandingVersion> findTopByLandingIdAndLocaleAndStatusOrderByVersionNumberDesc(
            UUID landingId, String locale, VersionStatus status);

    /** Fallback sense locale (backward compat) */
    Optional<LandingVersion> findTopByLandingIdAndStatusOrderByVersionNumberDesc(
            UUID landingId, VersionStatus status);

    /** Versió publicada per locale — per render */
    Optional<LandingVersion> findByLandingIdAndLocaleAndStatus(
            UUID landingId, String locale, VersionStatus status);

    Optional<LandingVersion> findTopByLandingIdOrderByVersionNumberDesc(UUID landingId);

    long countByLandingId(UUID landingId);

    long countByLandingIdAndLocale(UUID landingId, String locale);

    List<LandingVersion> findByLandingIdAndStatus(UUID landingId, VersionStatus status);
}
