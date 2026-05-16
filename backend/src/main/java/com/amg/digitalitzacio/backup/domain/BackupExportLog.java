package com.amg.digitalitzacio.backup.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_export_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BackupExportLog {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 100)
    private String tenantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExportStatus status;

    @Column(length = 500)
    private String filePath;

    private Long fileSize;
    private Integer sectionsCount;

    @Column(length = 500)
    private String errorMessage;

    private Instant startedAt;
    private Instant completedAt;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
