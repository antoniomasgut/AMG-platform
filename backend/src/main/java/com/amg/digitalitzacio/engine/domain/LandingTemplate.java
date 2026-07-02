package com.amg.digitalitzacio.engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "landing_templates")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LandingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 60, unique = true)
    private String slug;

    @Column(length = 255)
    private String description;

    /** Estils visuals per defecte (JSON): primaryColor, accentColor, fontHeading, fontBody, etc. */
    @Column(name = "default_styles", columnDefinition = "TEXT")
    private String defaultStyles;

    /** Paletes de color+tipografia predefinides (JSON array de {name, primary, accent, fontHeading, fontBody}). */
    @Column(name = "color_schemes", columnDefinition = "TEXT")
    private String colorSchemes;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
