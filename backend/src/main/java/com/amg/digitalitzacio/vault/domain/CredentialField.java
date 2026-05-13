package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credential_fields")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CredentialField {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "service_id", nullable = false) private UUID serviceId;
    @Column(nullable = false, length = 100) private String key;
    @Column(nullable = false, length = 150) private String label;
    @Enumerated(EnumType.STRING) private FieldType type;
    @Builder.Default @Column(nullable = false) private Boolean isRequired = true;
    @Column(length = 255) private String placeholder;
    @Column(length = 255) private String validationRegex;
    private Integer sortOrder;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
