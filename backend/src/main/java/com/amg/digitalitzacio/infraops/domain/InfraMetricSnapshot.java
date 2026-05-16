package com.amg.digitalitzacio.infraops.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "infra_metric_snapshots",
        indexes = @Index(name = "idx_infra_metric_collected_at", columnList = "collected_at"))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InfraMetricSnapshot {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    private Double cpuPercent;
    private Double ramPercent;
    private Double diskPercent;

    private Long ramUsedMb;
    private Long ramTotalMb;
    private Long diskUsedGb;
    private Long diskTotalGb;

    private Integer dbActiveConnections;
    private Integer dbMaxConnections;
    private Integer activeTenants;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;
}
