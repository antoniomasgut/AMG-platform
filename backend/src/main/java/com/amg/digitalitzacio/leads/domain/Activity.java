package com.amg.digitalitzacio.leads.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lead_activities")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Activity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID leadId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private ActivityType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private Instant dueDate;

    private Instant completedAt;

    @CreatedDate
    private Instant createdAt;
}
