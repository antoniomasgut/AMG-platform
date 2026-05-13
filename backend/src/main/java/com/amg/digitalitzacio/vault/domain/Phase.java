package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phases")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Phase {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "profile_id", nullable = false) private UUID profileId;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 255) private String description;
    @Column(nullable = false) private Integer sortOrder;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
