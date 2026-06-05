package com.amg.digitalitzacio.metaads.application;

import com.amg.digitalitzacio.metaads.api.dto.GenerateImageRequest;
import com.amg.digitalitzacio.metaads.api.dto.ImageUploadResponse;
import com.amg.digitalitzacio.metaads.domain.MetaAdsConfig;
import com.amg.digitalitzacio.metaads.domain.MetaAdsConfigRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaAdsImageGeneratorService {

    private static final String OPENAI_BASE    = "https://api.openai.com";
    private static final String GRAPH_BASE     = "https://graph.facebook.com";
    private static final String API_VERSION    = "v19.0";

    // DALL-E 3 soporta exactament aquests tres mides
    private static final Map<String, String> FORMAT_SIZE = Map.of(
        "FEED",    "1792x1024",   // Facebook/Instagram feed (landscape)
        "SQUARE",  "1024x1024",   // Instagram feed (quadrat)
        "STORY",   "1024x1792",   // Instagram Story / Reels (portrait)
        "BANNER",  "1792x1024"    // Facebook cover / banner
    );

    private static final Map<String, String> FORMAT_LABEL = Map.of(
        "FEED",   "Facebook/Instagram Feed (horitzontal 16:9)",
        "SQUARE", "Instagram Feed (quadrat 1:1)",
        "STORY",  "Instagram Story / Reels (vertical 9:16)",
        "BANNER", "Facebook Cover / Banner (horitzontal)"
    );

    private final MetaAdsConfigRepository configRepository;
    private final SystemConfigService sysConfig;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public ImageUploadResponse generateAndUpload(UUID tenantId, GenerateImageRequest req) throws Exception {
        String openAiKey = sysConfig.get("OPENAI_API_KEY");
        if (openAiKey == null || openAiKey.isBlank())
            throw new IllegalStateException("OPENAI_API_KEY no configurat al sistema");

        var config = configRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Meta Ads no configurat per aquest tenant"));
        String metaToken = resolveToken(tenantId, config);
        if (metaToken == null) throw new IllegalStateException("Token Meta Ads no configurat");

        String accountId = normalizeAccountId(config.getAdAccountId());
        String size = FORMAT_SIZE.getOrDefault(req.format(), "1024x1024");
        String enhancedPrompt = buildPrompt(req);

        // 1. Generar imatge amb DALL-E 3
        log.info("[ImageGen] Generant imatge DALL-E 3 | format={} mida={} tenant={}", req.format(), size, tenantId);
        byte[] imageBytes = generateWithDalle(openAiKey, enhancedPrompt, size);

        // 2. Pujar al compte de Meta Ads
        return uploadToMeta(accountId, metaToken, imageBytes, req.format());
    }

    private byte[] generateWithDalle(String apiKey, String prompt, String size) throws Exception {
        var rc = restClientBuilder.baseUrl(OPENAI_BASE).build();
        var requestBody = Map.of(
            "model",   "dall-e-3",
            "prompt",  prompt,
            "n",       1,
            "size",    size,
            "quality", "standard",
            "response_format", "url"
        );

        String raw = rc.post()
                .uri("/v1/images/generations")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(requestBody))
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(raw);
        String imageUrl = root.path("data").get(0).path("url").asText();
        log.info("[ImageGen] Imatge generada: {}", imageUrl.substring(0, Math.min(80, imageUrl.length())));

        // Descarregar bytes de la URL temporal de DALL-E
        return restClientBuilder.build()
                .get()
                .uri(imageUrl)
                .retrieve()
                .body(byte[].class);
    }

    private ImageUploadResponse uploadToMeta(String accountId, String token, byte[] imageBytes, String format) throws Exception {
        var form = new LinkedMultiValueMap<String, Object>();
        String filename = "generated_" + format.toLowerCase() + ".png";
        form.add(filename, new ByteArrayResource(imageBytes) {
            @Override public String getFilename() { return filename; }
        });

        var rc = restClientBuilder.baseUrl(GRAPH_BASE).build();
        String raw = rc.post()
                .uri("/" + API_VERSION + "/" + accountId + "/adimages?access_token=" + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(String.class);

        JsonNode images = objectMapper.readTree(raw).path("images");
        JsonNode first = images.fields().hasNext() ? images.fields().next().getValue() : null;
        if (first == null) throw new RuntimeException("Resposta inesperada de Meta en pujar imatge generada");

        return new ImageUploadResponse(first.path("hash").asText(), first.path("url").asText());
    }

    private String buildPrompt(GenerateImageRequest req) {
        String base = req.prompt() != null ? req.prompt() : "professional marketing image";
        String formatCtx = FORMAT_LABEL.getOrDefault(req.format(), "");

        String styleInstruction = switch (req.style() != null ? req.style() : "REALISTIC") {
            case "ILLUSTRATED" -> "flat design illustration style, clean vector art, vibrant colors";
            case "MINIMAL"     -> "minimalist design, clean white background, simple geometric shapes, professional";
            case "CINEMATIC"   -> "cinematic photography, dramatic lighting, professional product photo";
            default            -> "photorealistic, professional marketing photo, high quality, well lit";
        };

        return String.format(
            "Create a professional marketing image for %s. Content: %s. Style: %s. " +
            "No text overlays. Suitable for social media advertising. High quality, eye-catching.",
            formatCtx, base, styleInstruction
        );
    }

    private String resolveToken(UUID tenantId, MetaAdsConfig config) {
        if (config.getAccessToken() != null && !config.getAccessToken().isBlank())
            return config.getAccessToken();
        return sysConfig.get("META_ADS_MANAGEMENT_TOKEN");
    }

    private String normalizeAccountId(String id) {
        if (id == null) return null;
        return id.startsWith("act_") ? id : "act_" + id;
    }
}
