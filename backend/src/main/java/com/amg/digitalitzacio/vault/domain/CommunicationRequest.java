package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communication_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunicationRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "tenant_service_id", nullable = false) private UUID tenantServiceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CommunicationChannel channel;
    @Column(nullable = false, length = 200) private String recipient;
    @Column(length = 200) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CommunicationStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private RequestType requestType;
    @Column(name = "field_id") private UUID fieldId;
    @Column(name = "response_data", columnDefinition = "TEXT") private String responseData;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "responded_at") private Instant respondedAt;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
}
