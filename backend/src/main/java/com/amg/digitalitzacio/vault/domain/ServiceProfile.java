package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 100, unique = true) private String name;
    @Column(nullable = false, length = 60, unique = true) private String slug;
    @Column(length = 255) private String description;
    @Builder.Default @Column(nullable = false) private Boolean isActive = true;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
