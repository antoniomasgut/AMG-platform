package com.amg.digitalitzacio.engine.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LandingVisitDailyRepository extends JpaRepository<LandingVisitDaily, LandingVisitDaily.Key> {

    /** Upsert atòmic del comptador diari (evita race conditions entre requests concurrents). */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO landing_visit_daily (landing_id, visit_date, source, views)
        VALUES (:landingId, :visitDate, :source, 1)
        ON CONFLICT (landing_id, visit_date, source)
        DO UPDATE SET views = landing_visit_daily.views + 1
        """, nativeQuery = true)
    void incrementVisit(@Param("landingId") UUID landingId,
                        @Param("visitDate") LocalDate visitDate,
                        @Param("source") String source);

    /** Visites agregades per font de totes les landings d'un tenant des d'una data. */
    @Query(value = """
        SELECT v.source AS source, SUM(v.views) AS views
        FROM landing_visit_daily v
        JOIN landings l ON l.id = v.landing_id
        WHERE l.tenant_id = :tenantId AND v.visit_date >= :since
        GROUP BY v.source
        ORDER BY SUM(v.views) DESC
        """, nativeQuery = true)
    List<SourceViews> sumByTenantSince(@Param("tenantId") UUID tenantId,
                                       @Param("since") LocalDate since);

    interface SourceViews {
        String getSource();
        long getViews();
    }
}
