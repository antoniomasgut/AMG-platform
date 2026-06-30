package com.amg.digitalitzacio.social.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_meta_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialMetaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tenantId;

    @Column(length = 100)
    private String facebookPageId;

    @Column(length = 100)
    private String instagramAccountId;

    /** Long-lived Page Access Token xifrat AES-256 */
    @Column(columnDefinition = "TEXT")
    private String pageAccessTokenEncrypted;

    private Instant tokenExpiresAt;

    @Column(name = "pages_manage_posts_granted", nullable = false)
    @Builder.Default
    private Boolean pagesManagedPostsGranted = false;

    @Column(name = "ig_content_publish_granted", nullable = false)
    @Builder.Default
    private Boolean igContentPublishGranted = false;

    @Builder.Default
    private Instant connectedAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
