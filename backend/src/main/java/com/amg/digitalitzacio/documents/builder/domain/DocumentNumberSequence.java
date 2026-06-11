package com.amg.digitalitzacio.documents.builder.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_number_sequences")
@IdClass(DocumentNumberSequence.SeqKey.class)
@Getter @Setter
public class DocumentNumberSequence {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    @Column(name = "doc_type", length = 20)
    private String docType;

    @Id
    @Column(name = "year")
    private Integer year;

    @Column(name = "next_number", nullable = false)
    private Integer nextNumber = 1;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeqKey implements Serializable {
        private UUID tenantId;
        private String docType;
        private Integer year;
    }
}
