package com.amg.digitalitzacio.engine.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LandingRepository extends JpaRepository<Landing, UUID> {
    Page<Landing> findByTenantId(UUID tenantId, Pageable pageable);
    Optional<Landing> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Landing> findBySlug(String slug);
    Optional<Landing> findByPreviewToken(String previewToken);
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);
    long countByTenantId(UUID tenantId);
    long countByStatus(LandingStatus status);

    @Query("SELECT l FROM Landing l WHERE l.tenantId = :tenantId AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.slug) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Landing> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);
}
