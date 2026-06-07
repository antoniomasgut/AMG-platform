package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CommunicationRequestRepository extends JpaRepository<CommunicationRequest, UUID> {
    List<CommunicationRequest> findByTenantServiceIdOrderByCreatedAtDesc(UUID tenantServiceId);
    List<CommunicationRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, CommunicationStatus status);
    @Query("SELECT r FROM CommunicationRequest r WHERE r.status = 'SENT' AND r.expiresAt IS NOT NULL AND r.expiresAt < :now AND r.retryCount < r.maxRetries")
    List<CommunicationRequest> findExpiredForRetry(@Param("now") Instant now);
}
