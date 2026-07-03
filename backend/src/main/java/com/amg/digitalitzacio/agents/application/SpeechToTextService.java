package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Transcripció de notes de veu de Telegram (F5: "dicta una nota de veu i queda
 * documentada"). Baixa el fitxer amb el bot de plataforma i el transcriu via
 * una API compatible amb Whisper (OpenAI per defecte; Groq i altres funcionen
 * canviant STT_BASE_URL).
 *
 * Sense STT_API_KEY configurada, retorna empty i el webhook informa l'usuari.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpeechToTextService {

    private final SystemConfigService sysConfig;

    public boolean isConfigured() {
        String key = sysConfig.get("STT_API_KEY");
        return key != null && !key.isBlank();
    }

    /** Baixa la nota de veu de Telegram (bot de plataforma) i la transcriu. */
    public Optional<String> transcribeTelegramVoice(String fileId) {
        if (!isConfigured()) return Optional.empty();
        try {
            String botToken = sysConfig.get("TELEGRAM_BOT_TOKEN");
            if (botToken == null || botToken.isBlank()) return Optional.empty();

            // 1. Resoldre el path del fitxer
            var tg = RestClient.builder().baseUrl("https://api.telegram.org").build();
            JsonNode fileInfo = tg.get()
                    .uri("/bot{token}/getFile?file_id={fid}", botToken, fileId)
                    .retrieve().body(JsonNode.class);
            String filePath = fileInfo != null ? fileInfo.path("result").path("file_path").asText(null) : null;
            if (filePath == null) return Optional.empty();

            // 2. Baixar l'àudio (OGG/Opus normalment)
            byte[] audio = tg.get()
                    .uri("/file/bot{token}/{path}", botToken, filePath)
                    .retrieve().body(byte[].class);
            if (audio == null || audio.length == 0) return Optional.empty();

            // 3. Transcriure
            return transcribe(audio, filePath.substring(filePath.lastIndexOf('/') + 1));
        } catch (Exception e) {
            log.warn("[STT] Error transcrivint nota de veu {}: {}", fileId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> transcribe(byte[] audio, String filename) {
        try {
            String apiKey  = sysConfig.get("STT_API_KEY");
            String baseUrl = orDefault(sysConfig.get("STT_BASE_URL"), "https://api.openai.com/v1");
            String model   = orDefault(sysConfig.get("STT_MODEL"), "whisper-1");

            var form = new LinkedMultiValueMap<String, Object>();
            form.add("model", model);
            form.add("file", new ByteArrayResource(audio) {
                @Override public String getFilename() { return filename; }
            });

            var client = RestClient.builder().baseUrl(baseUrl).build();
            JsonNode response = client.post()
                    .uri("/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve().body(JsonNode.class);

            String text = response != null ? response.path("text").asText(null) : null;
            if (text == null || text.isBlank()) return Optional.empty();
            log.info("[STT] Nota de veu transcrita ({} bytes → {} chars)", audio.length, text.length());
            return Optional.of(text.trim());
        } catch (Exception e) {
            log.warn("[STT] Error a l'API de transcripció: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String orDefault(String v, String def) {
        return v != null && !v.isBlank() ? v : def;
    }
}
