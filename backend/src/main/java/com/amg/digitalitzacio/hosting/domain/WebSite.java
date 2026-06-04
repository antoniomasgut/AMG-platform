package com.amg.digitalitzacio.hosting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "websites")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class WebSite {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebsiteType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebsiteStatus status = WebsiteStatus.PENDING_REVIEW;

    @Column(length = 255)
    private String domain;

    @Column(length = 100)
    private String containerName;

    @Column(length = 100)
    private String upstreamContainer;

    private Integer upstreamPort;

    private Long storageBytes;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 500)
    private String contactRedirectUrl;

    @Column(columnDefinition = "TEXT")
    private String clientNotes;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    private UUID reviewedBy;

    private Instant reviewedAt;

    private Instant deployedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
