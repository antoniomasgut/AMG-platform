package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "followup_logs")
@Getter @Setter
public class FollowupLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false, length = 20) private String type;
    @Column(nullable = false) private UUID entityId;
    @Column(length = 255) private String contact;
    @Column(nullable = false) private Instant sentAt = Instant.now();
}
