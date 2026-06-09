package com.amg.digitalitzacio.engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "landing_versions", uniqueConstraints = @UniqueConstraint(name = "uq_landing_version_locale", columnNames = {"landing_id", "locale", "version_number"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LandingVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "landing_id", nullable = false) private UUID landingId;
    @Column(name = "version_number", nullable = false) private Integer versionNumber;
    /** ca | es | en | de */
    @Column(nullable = false, length = 5)
    @Builder.Default
    private String locale = "ca";
    @Enumerated(EnumType.STRING) @Column(nullable = false) private VersionStatus status;
    @Column(columnDefinition = "TEXT", nullable = false) private String content;
    @Column(columnDefinition = "TEXT") private String styles;
    private Instant publishedAt;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
}
