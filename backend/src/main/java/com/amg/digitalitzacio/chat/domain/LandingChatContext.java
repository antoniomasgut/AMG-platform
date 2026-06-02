package com.amg.digitalitzacio.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "landing_chat_contexts")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LandingChatContext {

    @Id
    @Column(name = "landing_id")
    private UUID landingId;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(length = 50)
    private String sector;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "profanity_action", nullable = false, length = 20)
    @Builder.Default
    private String profanityAction = "CLOSE";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
