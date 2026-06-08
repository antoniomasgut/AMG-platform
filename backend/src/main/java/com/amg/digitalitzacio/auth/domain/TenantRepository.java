package com.amg.digitalitzacio.auth.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Optional<Tenant> findByIsOwnerTrue();
    boolean existsByIsOwnerTrue();

    @Query("SELECT t FROM Tenant t WHERE t.contractedPhases LIKE %:phase%")
    List<Tenant> findByContractedPhasesContaining(@Param("phase") String phase);

    @Query(value = """
        SELECT * FROM tenants t
        WHERE (CAST(:search AS text) IS NULL
               OR lower(t.name) LIKE '%' || lower(CAST(:search AS text)) || '%'
               OR lower(t.slug) LIKE '%' || lower(CAST(:search AS text)) || '%')
          AND (CAST(:isActive AS boolean) IS NULL OR t.is_active = CAST(:isActive AS boolean))
          AND (CAST(:isFree AS boolean) IS NULL OR t.is_free = CAST(:isFree AS boolean))
          AND (t.is_owner IS NULL OR t.is_owner = false)
        ORDER BY t.created_at DESC
        """,
        countQuery = """
        SELECT count(*) FROM tenants t
        WHERE (CAST(:search AS text) IS NULL
               OR lower(t.name) LIKE '%' || lower(CAST(:search AS text)) || '%'
               OR lower(t.slug) LIKE '%' || lower(CAST(:search AS text)) || '%')
          AND (CAST(:isActive AS boolean) IS NULL OR t.is_active = CAST(:isActive AS boolean))
          AND (CAST(:isFree AS boolean) IS NULL OR t.is_free = CAST(:isFree AS boolean))
          AND (t.is_owner IS NULL OR t.is_owner = false)
        """,
        nativeQuery = true)
    Page<Tenant> findFiltered(
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            @Param("isFree") Boolean isFree,
            Pageable pageable);
}
