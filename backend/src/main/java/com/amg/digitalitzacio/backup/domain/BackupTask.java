package com.amg.digitalitzacio.backup.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_tasks")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BackupTask {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BackupTaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupScope scope;

    // Nullable: només per MANUAL_TENANT
    private UUID tenantId;

    // Nullable: null per a tasques programades
    private UUID requestedBy;

    private Integer totalTenants;
    private Integer completedTenants;
    private Integer failedTenants;

    @Column(length = 500)
    private String filePath;

    private Long fileSize;

    @Column(length = 500)
    private String archivePath;

    private Long archiveSize;

    @Column(length = 1000)
    private String errorMessage;

    private Instant startedAt;
    private Instant completedAt;
    private Instant retentionUntil;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
