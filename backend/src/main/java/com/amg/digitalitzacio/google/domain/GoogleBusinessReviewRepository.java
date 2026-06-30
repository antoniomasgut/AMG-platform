package com.amg.digitalitzacio.google.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoogleBusinessReviewRepository extends JpaRepository<GoogleBusinessReview, UUID> {
    List<GoogleBusinessReview> findByTenantIdAndRatingGreaterThanEqualOrderByRatingDescReviewTimeDesc(UUID tenantId, int minRating);
    Optional<GoogleBusinessReview> findByTenantIdAndReviewId(UUID tenantId, String reviewId);
    void deleteByTenantId(UUID tenantId);
    int countByTenantId(UUID tenantId);
}
