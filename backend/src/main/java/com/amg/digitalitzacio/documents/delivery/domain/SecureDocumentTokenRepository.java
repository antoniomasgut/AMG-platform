package com.amg.digitalitzacio.documents.delivery.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecureDocumentTokenRepository extends JpaRepository<SecureDocumentToken, UUID> {

    Optional<SecureDocumentToken> findByToken(String token);

    Page<SecureDocumentToken> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SecureDocumentToken t WHERE t.sensitivity = 'STANDARD' AND t.expiresAt < :cutoff")
    int deleteExpiredStandard(Instant cutoff);

    @Modifying
    @Query("DELETE FROM SecureDocumentToken t WHERE t.sensitivity = 'SENSITIVE' AND t.expiresAt < :cutoff")
    int deleteExpiredSensitive(Instant cutoff);
}
