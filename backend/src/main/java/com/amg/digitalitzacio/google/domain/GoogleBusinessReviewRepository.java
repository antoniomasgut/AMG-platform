package com.amg.digitalitzacio.google.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoogleBusinessReviewRepository extends JpaRepository<GoogleBusinessReview, UUID> {
    List<GoogleBusinessReview> findByTenantIdAndRatingGreaterThanEqualOrderByRatingDescReviewTimeDesc(UUID tenantId, int minRating);
    Optional<GoogleBusinessReview> findByTenantIdAndReviewId(UUID tenantId, String reviewId);
    void deleteByTenantId(UUID tenantId);
    int countByTenantId(UUID tenantId);
    // Ressenyes noves pendents de notificar per Telegram (Mòdul 54)
    List<GoogleBusinessReview> findByTenantIdAndNotifiedAtIsNullOrderByReviewTimeDesc(UUID tenantId);
    // Ressenyes negatives sense resposta, notificades abans del llindar (Mòdul 57 F2)
    List<GoogleBusinessReview> findByTenantIdAndRatingLessThanEqualAndReplyIsNullAndNotifiedAtBefore(
        UUID tenantId, int maxRating, Instant notifiedBefore);
}
