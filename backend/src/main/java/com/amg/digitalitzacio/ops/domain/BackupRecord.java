package com.amg.digitalitzacio.ops.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_records")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BackupRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private BackupType type;

    @Enumerated(EnumType.STRING)
    private BackupStatus status;

    @Column(length = 255)
    private String fileName;

    private Long fileSize;

    private Instant startedAt;

    private Instant completedAt;

    @Column(length = 500)
    private String errorMessage;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
