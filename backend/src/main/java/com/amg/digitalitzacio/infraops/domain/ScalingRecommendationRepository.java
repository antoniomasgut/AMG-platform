package com.amg.digitalitzacio.infraops.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScalingRecommendationRepository extends JpaRepository<ScalingRecommendation, UUID> {
    List<ScalingRecommendation> findByIsActiveTrueOrderByCreatedAtDesc();
    List<ScalingRecommendation> findAllByOrderByCreatedAtDesc();
    Optional<ScalingRecommendation> findTopByTypeOrderByCreatedAtDesc(RecommendationType type);
    boolean existsByTypeAndSentAtAfter(RecommendationType type, Instant after);
}
