package com.amg.digitalitzacio.metaads.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "campaign_spend",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "campaign_id", "spend_date"}))
@Getter
@Setter
public class CampaignSpend {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String campaignId;

    @Column(nullable = false, length = 200)
    private String campaignName;

    @Column(precision = 10, scale = 2)
    private BigDecimal spend;

    private Long impressions;

    private Long clicks;

    @Column(nullable = false)
    private LocalDate spendDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
