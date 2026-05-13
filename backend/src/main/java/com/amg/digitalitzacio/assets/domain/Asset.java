package com.amg.digitalitzacio.assets.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Asset {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 255, nullable = false)
    private String originalName;

    @Column(length = 100, nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long size;

    private Integer width;

    private Integer height;

    @Column(length = 500, nullable = false)
    private String storagePath;

    @Column(length = 500)
    private String thumbnailPath;

    @Column(length = 500, nullable = false)
    private String url;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
