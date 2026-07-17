package com.amg.digitalitzacio.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String nif;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_channel", length = 20)
    private PreferredChannel preferredChannel;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BusinessSector sector;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_size", length = 20)
    private BusinessSize businessSize;

    @Column(name = "contracted_phases", columnDefinition = "TEXT")
    private String contractedPhases; // comma-separated: "F1", "F2,F4", "F1,F3,F5"

    @Column(name = "active_phases", columnDefinition = "TEXT")
    private String activePhases; // null = totes les contractades actives; altrament sobreescriu

    /** Comprova si una fase és operativa. Mira activePhases; si null, consulta contractedPhases. */
    public boolean isPhaseActive(String phase) {
        var source = activePhases != null ? activePhases : contractedPhases;
        if (source == null || source.isBlank()) return false;
        for (String p : source.split(",")) {
            if (phase.equals(p.trim())) return true;
        }
        return false;
    }

    @Column(name = "agent_system_prompt", columnDefinition = "TEXT")
    private String agentSystemPrompt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFree = false;

    @Column(name = "is_owner", nullable = false)
    @Builder.Default
    private Boolean isOwner = false;

    @Column(name = "billing_start_date")
    private LocalDate billingStartDate;

    @Column(name = "implementation_delivered_at")
    private Instant implementationDeliveredAt;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
