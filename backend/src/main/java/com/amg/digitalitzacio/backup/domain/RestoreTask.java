package com.amg.digitalitzacio.backup.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restore_tasks")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RestoreTask {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    // Nullable: pot ser una restauració sense backupTask associat
    private UUID backupTaskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RestoreStatus status;

    // Array JSON de seccions emmagatzemat com a TEXT
    @Column(columnDefinition = "TEXT")
    private String sections;

    @Column(nullable = false)
    private UUID requestedBy;

    @Column(length = 1000)
    private String errorMessage;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    private Instant completedAt;
}
