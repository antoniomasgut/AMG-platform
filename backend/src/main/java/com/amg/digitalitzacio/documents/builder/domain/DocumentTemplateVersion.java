package com.amg.digitalitzacio.documents.builder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_template_versions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"template_id", "version"})
})
@Getter @Setter
public class DocumentTemplateVersion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String layout;

    @Column(name = "data_bindings", columnDefinition = "TEXT", nullable = false)
    private String dataBindings = "{}";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String styles = "{}";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
