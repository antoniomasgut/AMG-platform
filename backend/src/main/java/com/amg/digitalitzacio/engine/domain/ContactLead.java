package com.amg.digitalitzacio.engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_leads")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactLead {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "landing_id", nullable = false) private UUID landingId;
    @Column(length = 150) private String name;
    @Column(length = 200) private String email;
    @Column(length = 30) private String phone;
    @Column(columnDefinition = "TEXT") private String message;
    @Column(columnDefinition = "TEXT") private String metadata;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
}
