package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_team_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "telegram_user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantTeamMember {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "interaction_count", nullable = false)
    private int interactionCount;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    // Quan s'ha enviat el missatge d'upsell F5 (null = no enviat)
    @Column(name = "upsell_sent_at")
    private Instant upsellSentAt;
}
