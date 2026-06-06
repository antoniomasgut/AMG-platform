package com.amg.digitalitzacio.documents.builder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generated_documents",
    indexes = {
        @Index(name = "idx_generated_documents_tenant", columnList = "tenant_id"),
        @Index(name = "idx_generated_documents_template", columnList = "template_id"),
        @Index(name = "idx_generated_documents_status", columnList = "status")
    })
@Getter @Setter
public class GeneratedDocument {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private Integer templateVersion;

    @Column(nullable = false, length = 50)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @Column
    private UUID customerId;

    @Column(name = "customer_data", columnDefinition = "TEXT", nullable = false)
    private String customerData = "{}";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String variables = "{}";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String articles = "[]";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String calculated = "{}";

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    @Column(length = 255)
    private String pdfUrl;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { generatedAt = createdAt = Instant.now(); }
}
