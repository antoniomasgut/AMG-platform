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
    private Boolean isActive = false;

    @Builder.Default @Column(name = "widget_enabled", nullable = false)
    private Boolean widgetEnabled = false;

    @Builder.Default @Column(name = "whatsapp_enabled", nullable = false)
    private Boolean whatsappEnabled = false;

    @Builder.Default @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_mode", nullable = false)
    private AgentMode agentMode = AgentMode.AUTO;

    // Modes per canal (nullable → hereten agentMode global)
    @Enumerated(EnumType.STRING)
    @Column(name = "email_mode")
    private AgentMode emailMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "whatsapp_mode")
    private AgentMode whatsappMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_mode")
    private AgentMode widgetMode;

    @Column(name = "whatsapp_phone_number", length = 20)
    private String whatsappPhoneNumber;

    @Column(name = "whatsapp_meta_phone_number_id", length = 30)
    private String whatsappMetaPhoneNumberId;

    @Column(name = "email_address", length = 100)
    private String emailAddress;

    @Column(name = "meta_page_id", length = 30)
    private String metaPageId;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Mode efectiu per a un canal: override del canal si n'hi ha, si no el mode global. */
    public AgentMode modeFor(ConversationChannel channel) {
        AgentMode override = channel == null ? null : switch (channel) {
            case EMAIL                     -> emailMode;
            case WHATSAPP, WHATSAPP_META   -> whatsappMode;
            case WIDGET                    -> widgetMode;
            default                        -> null;
        };
        if (override != null) return override;
        return agentMode != null ? agentMode : AgentMode.AUTO;
    }
}
