package com.amg.digitalitzacio.metaads.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ad_creatives")
@Getter
@Setter
public class AdCreative {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 50)
    private String metaCreativeId;

    @Column(length = 200)
    private String name;

    @Column(length = 255)
    private String headline;

    @Column(length = 600)
    private String body;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String callToAction;

    @Column(length = 500)
    private String linkUrl;

    private UUID imageAssetId;

    @Column(length = 100)
    private String metaImageHash;

    @Column(length = 50)
    private String metaVideoId;

    @Column(length = 50)
    private String metaLeadFormId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
