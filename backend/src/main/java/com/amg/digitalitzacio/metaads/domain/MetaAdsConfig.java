package com.amg.digitalitzacio.metaads.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meta_ads_configs")
@Getter
@Setter
public class MetaAdsConfig {

    @Id
    private UUID tenantId;

    @Column(length = 50)
    private String adAccountId;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    private boolean enabled = false;

    private Instant lastSyncAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
