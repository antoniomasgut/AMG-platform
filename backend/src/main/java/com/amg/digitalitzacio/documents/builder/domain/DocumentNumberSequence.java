package com.amg.digitalitzacio.documents.builder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_number_sequences")
@Getter @Setter
public class DocumentNumberSequence {

    @Id
    private UUID tenantId;

    @Column(nullable = false, length = 10)
    private String prefix = "DOC";

    @Column(name = "next_number", nullable = false)
    private Integer nextNumber = 1;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
