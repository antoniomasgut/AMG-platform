package com.amg.digitalitzacio.telegram.application;

import com.amg.digitalitzacio.telegram.api.dto.TelegramConfigResponse;
import com.amg.digitalitzacio.telegram.api.dto.TelegramConnectRequest;
import com.amg.digitalitzacio.telegram.domain.TelegramConnectionStatus;
import com.amg.digitalitzacio.telegram.domain.TenantTelegramConfig;
import com.amg.digitalitzacio.telegram.domain.TenantTelegramConfigRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenantTelegramService {

    private final TenantTelegramConfigRepository configRepository;
    private final VaultEncryption vaultEncryption;
    private final SystemConfigService systemConfigService;

    @Value("${APP_DOMAIN:amgdl.com}")
    private String appDomain;

    @Transactional(readOnly = true)
    public TelegramConfigResponse getConfig(UUID tenantId) {
        return configRepository.findByTenantId(tenantId)
                .map(this::toResponse)
                .orElse(null);
    }

    public TelegramConfigResponse connect(UUID tenantId, TelegramConnectRequest req) {
        if (req.botToken() == null || req.botToken().isBlank()) {
            throw new IllegalArgumentException("El token del bot és obligatori");
        }

        var config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantTelegramConfig.builder().tenantId(tenantId).build());

        config.setBotTokenEncrypted(vaultEncryption.encrypt(req.botToken()));
        config.setStatus(TelegramConnectionStatus.PENDING);
        config.setWebhookRegistered(false);
        config.setBotUsername(null);
        config.setConnectedAt(null);

        // Obté info del bot i registra el webhook
        try {
            String username = fetchBotUsername(req.botToken());
            config.setBotUsername(username);

            String webhookUrl = "https://api." + appDomain + "/api/v1/agents/telegram/webhook/" + tenantId;
            boolean registered = registerWebhook(req.botToken(), webhookUrl);
            config.setWebhookRegistered(registered);

            if (registered) {
                config.setStatus(TelegramConnectionStatus.CONNECTED);
                config.setConnectedAt(Instant.now());
                log.info("Bot Telegram @{} configurat per tenant {} — webhook: {}", username, tenantId, webhookUrl);
            } else {
                config.setStatus(TelegramConnectionStatus.ERROR);
            }
        } catch (Exception e) {
            log.error("Error configurant bot Telegram per tenant {}: {}", tenantId, e.getMessage());
            config.setStatus(TelegramConnectionStatus.ERROR);
        }

        return toResponse(configRepository.save(config));
    }

    public TelegramConfigResponse verify(UUID tenantId) {
        var config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Bot Telegram no configurat per al tenant"));

        String token = vaultEncryption.decrypt(config.getBotTokenEncrypted());
        try {
            String username = fetchBotUsername(token);
            config.setBotUsername(username);

            String webhookUrl = "https://api." + appDomain + "/api/v1/agents/telegram/webhook/" + tenantId;
            boolean registered = registerWebhook(token, webhookUrl);
            config.setWebhookRegistered(registered);
            config.setStatus(registered ? TelegramConnectionStatus.CONNECTED : TelegramConnectionStatus.ERROR);
            if (registered) config.setConnectedAt(Instant.now());
        } catch (Exception e) {
            config.setStatus(TelegramConnectionStatus.ERROR);
            config.setWebhookRegistered(false);
            configRepository.save(config);
            throw new IllegalStateException("Token invàlid o error de Telegram: " + e.getMessage());
        }

        return toResponse(configRepository.save(config));
    }

    public void disconnect(UUID tenantId) {
        configRepository.findByTenantId(tenantId).ifPresent(config -> {
            try {
                String token = vaultEncryption.decrypt(config.getBotTokenEncrypted());
                deleteWebhook(token);
            } catch (Exception e) {
                log.warn("No s'ha pogut esborrar el webhook de Telegram per tenant {}: {}", tenantId, e.getMessage());
            }
            configRepository.delete(config);
        });
    }

    /** Retorna el token desxifrat — ús intern per enviar missatges */
    @Transactional(readOnly = true)
    public String getDecryptedToken(UUID tenantId) {
        return configRepository.findByTenantId(tenantId)
                .filter(c -> c.getStatus() == TelegramConnectionStatus.CONNECTED)
                .map(c -> vaultEncryption.decrypt(c.getBotTokenEncrypted()))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private String fetchBotUsername(String token) {
        var result = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + token)
                .build()
                .get()
                .uri("/getMe")
                .retrieve()
                .body(Map.class);
        if (result == null || !Boolean.TRUE.equals(result.get("ok"))) {
            throw new IllegalStateException("Resposta invàlida de Telegram");
        }
        var resultData = (Map<?, ?>) result.get("result");
        return (String) resultData.get("username");
    }

    @SuppressWarnings("unchecked")
    private boolean registerWebhook(String token, String webhookUrl) {
        // Si hi ha TELEGRAM_WEBHOOK_SECRET configurat, cal registrar el webhook amb el
        // secret_token perquè Telegram enviï el header X-Telegram-Bot-Api-Secret-Token;
        // sense això el controlador rebutjaria (401) les peticions d'aquest bot.
        var body = new java.util.HashMap<String, Object>();
        body.put("url", webhookUrl);
        String secret = systemConfigService.get("TELEGRAM_WEBHOOK_SECRET");
        if (secret != null && !secret.isBlank()) {
            body.put("secret_token", secret);
        }
        var result = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + token)
                .build()
                .post()
                .uri("/setWebhook")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        return result != null && Boolean.TRUE.equals(result.get("ok"));
    }

    @SuppressWarnings("unchecked")
    private void deleteWebhook(String token) {
        RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + token)
                .build()
                .post()
                .uri("/deleteWebhook")
                .retrieve()
                .body(Map.class);
    }

    private TelegramConfigResponse toResponse(TenantTelegramConfig c) {
        return new TelegramConfigResponse(
                c.getId(), c.getTenantId(),
                c.getBotUsername(),
                c.getStatus().name(),
                c.getWebhookRegistered(),
                c.getConnectedAt(),
                c.getCreatedAt()
        );
    }
}
