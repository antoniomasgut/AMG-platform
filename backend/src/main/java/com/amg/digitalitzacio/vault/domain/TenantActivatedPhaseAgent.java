package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_activated_phase_agents")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantActivatedPhaseAgent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "phase_id", nullable = false) private UUID phaseId;
    // WHATSAPP | TELEGRAM | EMAIL
    @Column(name = "agent_type", length = 30, nullable = false) private String agentType;
    @Column(length = 200) private String label;
    @Builder.Default @Column(length = 30) private String status = "PENDING";
    @CreatedDate @Column(updatable = false) private Instant createdAt;
}
