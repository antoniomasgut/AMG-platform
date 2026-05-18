package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_chat_links")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantChatLink {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "link_code", length = 20, unique = true)
    private String linkCode;

    @Column(name = "link_code_expires_at")
    private Instant linkCodeExpiresAt;

    @Builder.Default @Column(nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_mode", nullable = false)
    private AgentMode agentMode = AgentMode.AUTO;

    @Column(name = "whatsapp_phone_number", length = 20)
    private String whatsappPhoneNumber;

    @Column(name = "email_address", length = 100)
    private String emailAddress;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
