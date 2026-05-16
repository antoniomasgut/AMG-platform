package com.amg.digitalitzacio.infraops.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scaling_recommendations")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScalingRecommendation {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationSeverity severity;

    @Column(nullable = false, length = 500)
    private String message;

    private Double triggerValue;
    private Double threshold;

    private Instant sentAt;
    private Instant resolvedAt;

    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;
}
