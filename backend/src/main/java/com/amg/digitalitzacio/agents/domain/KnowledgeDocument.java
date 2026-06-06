package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeDocument {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "knowledge_base_id", nullable = false)
    private UUID knowledgeBaseId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Builder.Default
    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "uploaded_at", updatable = false, nullable = false)
    private Instant uploadedAt;
}
