package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.google.application.GoogleTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publica Local Posts a Google Business Profile via My Business API v4.9.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleBusinessPublisherService {

    private static final String GMB_URL = "https://mybusiness.googleapis.com/v4";

    private final GoogleTokenService googleTokenService;
    private final ObjectMapper objectMapper;

    /**
     * Publica un Local Post de tipus WHAT'S NEW.
     * @param tenantId    per obtenir el token OAuth del tenant
     * @param locationName nom de la ubicació (p.ex. "accounts/12345/locations/67890")
     * @param summary     text del post
     * @param mediaUrl    URL de la imatge (opcional, null si no n'hi ha)
     * @return ID del post creat
     */
    public String publishWhatsNew(UUID tenantId, String locationName, String summary, String mediaUrl) {
        String accessToken = getAccessToken(tenantId);
        var body = buildLocalPost("STANDARD", summary, null, null, mediaUrl);
        return postLocalPost(locationName, accessToken, body);
    }

    /**
     * Publica un Local Post de tipus OFFER.
     */
    public String publishOffer(UUID tenantId, String locationName, String summary, String mediaUrl) {
        String accessToken = getAccessToken(tenantId);
        var body = buildLocalPost("OFFER", summary, null, null, mediaUrl);
        return postLocalPost(locationName, accessToken, body);
    }

    /**
     * Publica un Local Post de tipus EVENT.
     */
    public String publishEvent(UUID tenantId, String locationName, String title, String summary, String mediaUrl) {
        String accessToken = getAccessToken(tenantId);
        var body = buildLocalPost("EVENT", summary, title, null, mediaUrl);
        return postLocalPost(locationName, accessToken, body);
    }

    /**
     * Puja una foto a la GALERIA del perfil (no com a post): apareix permanentment
     * a les fotos del negoci. Molt valuós per al SEO local. API v4 media.create.
     * @return nom del recurs de media creat
     */
    public String uploadPhotoToGallery(UUID tenantId, String locationName, String mediaUrl) {
        String accessToken = getAccessToken(tenantId);
        var body = Map.of(
            "mediaFormat", "PHOTO",
            "locationAssociation", Map.of("category", "ADDITIONAL"),
            "sourceUrl", mediaUrl
        );
        try {
            var raw = RestClient.create().post()
                .uri(GMB_URL + "/" + locationName + "/media")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(raw);
            if (node.has("error")) {
                throw new RuntimeException("GMB media error: " + node.path("error").path("message").asText());
            }
            return node.path("name").asText();
        } catch (Exception e) {
            log.error("Error pujant foto a la galeria GBP {}: {}", locationName, e.getMessage());
            throw new RuntimeException("Error pujant la foto a Google: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildLocalPost(String topicType, String summary,
                                                String title, String actionType, String mediaUrl) {
        var post = new HashMap<String, Object>();
        post.put("topicType", topicType);
        post.put("summary", summary);

        if (title != null && !title.isBlank()) {
            post.put("title", title);
        }

        if (mediaUrl != null && !mediaUrl.isBlank()) {
            post.put("media", new Object[]{Map.of(
                "mediaFormat", "PHOTO",
                "sourceUrl",   mediaUrl
            )});
        }

        if (actionType != null) {
            post.put("callToAction", Map.of("actionType", actionType));
        }

        return post;
    }

    private String postLocalPost(String locationName, String accessToken, Map<String, Object> body) {
        try {
            var raw = RestClient.create().post()
                .uri(GMB_URL + "/" + locationName + "/localPosts")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(raw);
            if (node.has("error")) {
                throw new RuntimeException("GMB API error: " + node.path("error").path("message").asText());
            }
            return node.path("name").asText();
        } catch (Exception e) {
            log.error("Error publicant Local Post a GBP {}: {}", locationName, e.getMessage());
            throw new RuntimeException("Error publicant a Google Business: " + e.getMessage(), e);
        }
    }

    private String getAccessToken(UUID tenantId) {
        return googleTokenService.getValidCredentials(tenantId).accessToken();
    }
}
