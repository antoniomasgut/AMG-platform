package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.shared.storage.StorageProviderRouter;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Descarrega una foto de Telegram i la puja al storage del tenant.
 * Retorna una URL signada (24h) vàlida per a Meta/GBP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramMediaUploadService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final SystemConfigService sysConfig;
    private final StorageProviderRouter storageRouter;
    private final ObjectMapper objectMapper;

    /**
     * @param fileId   file_id rebut de Telegram
     * @param tenantId per triar el storage provider del tenant
     * @return URL pública/signada (24h) de la imatge al storage
     */
    public String downloadAndUpload(String fileId, UUID tenantId) {
        String token = sysConfig.get("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new RuntimeException("TELEGRAM_BOT_TOKEN no configurat");
        }

        String filePath = getFilePath(token, fileId);
        byte[] imageBytes = downloadFile(token, filePath);

        String extension = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf('.')) : ".jpg";
        String fileName = "social-" + UUID.randomUUID() + extension;

        var provider = storageRouter.getProvider(tenantId);
        var stored = provider.upload(
            new java.io.ByteArrayInputStream(imageBytes),
            fileName,
            "image/jpeg"
        );

        String signedUrl = provider.getSignedUrl(stored.fileId(), Duration.ofHours(24));
        log.info("Foto TG {} pujada a storage per tenant {} → {}", fileId, tenantId, stored.fileId());
        return signedUrl;
    }

    private String getFilePath(String token, String fileId) {
        try {
            var raw = RestClient.create().get()
                .uri("https://api.telegram.org/bot" + token + "/getFile?file_id=" + fileId)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            if (!root.path("ok").asBoolean()) {
                throw new RuntimeException("Telegram getFile error: " + raw);
            }
            return root.path("result").path("file_path").asText();
        } catch (Exception e) {
            log.error("Error obtenint file_path de Telegram per file_id {}: {}", fileId, e.getMessage());
            throw new RuntimeException("No s'ha pogut obtenir el fitxer de Telegram", e);
        }
    }

    private byte[] downloadFile(String token, String filePath) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.telegram.org/file/bot" + token + "/" + filePath))
                .GET()
                .build();
            var response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Error descarregant foto de Telegram: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            log.error("Error descarregant foto de Telegram {}: {}", filePath, e.getMessage());
            throw new RuntimeException("No s'ha pogut descarregar la foto de Telegram", e);
        }
    }
}
