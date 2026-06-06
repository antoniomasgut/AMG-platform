package com.amg.digitalitzacio.documents.builder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_templates")
@Getter @Setter
public class DocumentTemplate {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType documentType;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String layout = "[]";

    @Column(name = "data_bindings", columnDefinition = "TEXT", nullable = false)
    private String dataBindings = "{}";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String styles = "{}";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
