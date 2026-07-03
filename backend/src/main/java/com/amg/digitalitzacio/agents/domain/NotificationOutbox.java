package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Notificacions Telegram del bot de plataforma que han fallat en enviar-se.
 * Un scheduler les reintenta (màx 5 intents) — cap avís crític es perd.
 */
@Entity
@Table(name = "notification_outbox")
@Getter @Setter
public class NotificationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(columnDefinition = "text")
    private String replyMarkup;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false, length = 10)
    private String status = "PENDING";

    @Column(length = 300)
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant sentAt;
}
