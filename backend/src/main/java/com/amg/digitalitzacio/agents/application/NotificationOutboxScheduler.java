package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Reintenta cada 5 minuts les notificacions Telegram de plataforma que van
 * fallar en el moment d'enviar-se (Telegram caigut, timeout de xarxa...).
 * Màxim 5 intents; després queden marcades FAILED per a revisió manual.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxScheduler {

    private static final int MAX_ATTEMPTS = 5;

    private final NotificationOutboxRepository outboxRepository;
    private final TelegramBotClient telegramBotClient;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void retryPending() {
        var pending = outboxRepository.findTop20ByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) return;

        for (var entry : pending) {
            boolean sent = telegramBotClient.retryOutboxEntry(entry);
            if (sent) {
                entry.setStatus("SENT");
                entry.setSentAt(Instant.now());
            } else {
                entry.setAttempts(entry.getAttempts() + 1);
                entry.setLastError("Reintent " + entry.getAttempts() + " fallit");
                if (entry.getAttempts() >= MAX_ATTEMPTS) {
                    entry.setStatus("FAILED");
                    log.error("[Outbox] Notificació TG descartada després de {} intents (chat {})",
                            MAX_ATTEMPTS, entry.getChatId());
                }
            }
            outboxRepository.save(entry);
        }
        log.info("[Outbox] Reintents processats: {}", pending.size());
    }
}
