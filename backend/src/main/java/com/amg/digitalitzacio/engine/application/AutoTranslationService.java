package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Tradueix el JSON de contingut d'una landing via Claude Haiku.
 * Preserva l'estructura JSON; només tradueix valors textuals llegibles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoTranslationService {

    private static final String ANTHROPIC_BASE    = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL             = "claude-haiku-4-5-20251001";
    private static final int    MAX_TOKENS        = 8192;

    private static final Map<String, String> LOCALE_NAMES = Map.of(
        "ca", "català", "es", "castellà", "en", "anglès", "de", "alemany"
    );

    private final SystemConfigService sysConfig;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public String translateContent(String contentJson, String fromLocale, String toLocale) {
        if (contentJson == null || contentJson.isBlank()) return contentJson;
        try {
            String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("ANTHROPIC_API_KEY no configurat, traducció omesa");
                return contentJson;
            }
            var fromName = LOCALE_NAMES.getOrDefault(fromLocale, fromLocale);
            var toName   = LOCALE_NAMES.getOrDefault(toLocale, toLocale);
            var result   = callClaude(apiKey, buildPrompt(contentJson, fromName, toName));
            return extractJson(result, contentJson);
        } catch (Exception e) {
            log.error("Error traduint contingut {}->{}: {}", fromLocale, toLocale, e.getMessage());
            return contentJson;
        }
    }

    private String buildPrompt(String contentJson, String fromName, String toName) {
        return "Ets un traductor professional especialitzat en contingut web per a petites empreses.\n\n" +
               "Se't proporcionarà un JSON que representa el contingut d'una pàgina web.\n" +
               "Tradueix tots els valors de text llegibles del " + fromName + " al " + toName + ".\n\n" +
               "Regles ESTRICTES:\n" +
               "- Tradueix: títols, subtítols, descripcions, text de botons, preguntes, respostes, testimonis, etiquetes\n" +
               "- NO tradueixis: URLs (valors que comencen per http, https, # o /), colors CSS (#rrggbb), strings buits \"\", números\n" +
               "- Mantén EXACTAMENT la mateixa estructura JSON (claus, tipus, nivells d'anidament)\n" +
               "- Retorna ÚNICAMENT el JSON traduït, sense cap text addicional ni bloc de codi markdown\n\n" +
               "JSON a traduir:\n" + contentJson;
    }

    private String extractJson(String response, String fallback) {
        if (response == null || response.isBlank()) return fallback;
        var trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end   = trimmed.lastIndexOf("```");
            if (end > start) trimmed = trimmed.substring(start, end).trim();
        }
        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (Exception e) {
            log.warn("Resposta de traducció no és JSON vàlid, usant contingut original");
            return fallback;
        }
    }

    private String callClaude(String apiKey, String userMessage) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
            "model",      MODEL,
            "max_tokens", MAX_TOKENS,
            "messages",   List.of(Map.of("role", "user", "content", userMessage))
        ));
        var rc  = restClientBuilder.baseUrl(ANTHROPIC_BASE).build();
        var raw = rc.post()
            .uri("/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .body(String.class);
        return objectMapper.readTree(raw).path("content").path(0).path("text").asText("");
    }
}
