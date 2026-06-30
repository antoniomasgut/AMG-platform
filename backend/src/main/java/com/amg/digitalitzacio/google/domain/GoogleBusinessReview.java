package com.amg.digitalitzacio.google.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_business_reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "review_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoogleBusinessReview {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "review_id", nullable = false)
    private String reviewId;

    @Column(name = "author_name")
    private String authorName;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "review_time")
    private Instant reviewTime;

    @Column(name = "synced_at")
    private Instant syncedAt;
}
