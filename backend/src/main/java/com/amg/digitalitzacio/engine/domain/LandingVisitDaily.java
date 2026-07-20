package com.amg.digitalitzacio.engine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comptador diari de visites d'una landing per font de trànsit (utm_source normalitzat).
 * Agregat sense PII — només nombres per data i font.
 */
@Entity
@Table(name = "landing_visit_daily")
@IdClass(LandingVisitDaily.Key.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class LandingVisitDaily {

    @Id @Column(name = "landing_id") private UUID landingId;
    @Id @Column(name = "visit_date") private LocalDate visitDate;
    @Id private String source;

    @Column(nullable = false) private int views;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Key implements Serializable {
        private UUID landingId;
        private LocalDate visitDate;
        private String source;

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return java.util.Objects.equals(landingId, k.landingId)
                && java.util.Objects.equals(visitDate, k.visitDate)
                && java.util.Objects.equals(source, k.source);
        }
        @Override public int hashCode() {
            return java.util.Objects.hash(landingId, visitDate, source);
        }
    }
}
