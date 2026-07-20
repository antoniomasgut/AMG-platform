package com.amg.digitalitzacio.social.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    /** INSTAGRAM | FACEBOOK | GOOGLE_BUSINESS */
    @Column(nullable = false, length = 30)
    private String network;

    /** FEED_PHOTO | POST_TEXT | POST_PHOTO | WHATS_NEW | OFFER | EVENT */
    @Column(nullable = false, length = 30)
    private String postType;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(length = 255)
    private String externalPostId;

    @Column(columnDefinition = "TEXT")
    private String externalPostUrl;

    /** DRAFT | SCHEDULED | PUBLISHED | FAILED | CANCELLED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    private Instant scheduledAt;
    private Instant publishedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Mètriques d'engagement (Mòdul 55, feature 2) — sincronitzades des de Graph API
    private Integer reach;
    private Integer likes;
    private Integer comments;
    private Instant metricsSyncedAt;

    /** Referència inversa al Content Planner (Mòdul 58): l'item que ha originat aquest post. */
    @Column(name = "content_plan_item_id")
    private UUID contentPlanItemId;

    /** Nombre de reintents realitzats (auto-retry en FAILED, max 2 → 3 intents totals). */
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
