package com.amg.digitalitzacio.social.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Publica fotos a Instagram via Meta Graph API (Content Publishing API).
 * Flux: POST /{ig-user-id}/media → POST /{ig-user-id}/media_publish
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramPublisherService {

    private static final String GRAPH_URL  = "https://graph.facebook.com";
    private static final String API_VERSION = "v22.0";

    private final ObjectMapper objectMapper;

    /**
     * @param igUserId   instagram_account_id del tenant
     * @param accessToken page access token desxifrat
     * @param mediaUrl   URL pública accessible des d'internet (MinIO o CDN)
     * @param caption    text del post
     * @return ID extern del post publicat
     */
    public String publishFeedPhoto(String igUserId, String accessToken, String mediaUrl, String caption) {
        String containerId = createMediaContainer(igUserId, accessToken, mediaUrl, caption);
        return publishContainer(igUserId, accessToken, containerId);
    }

    private String createMediaContainer(String igUserId, String accessToken, String mediaUrl, String caption) {
        var body = Map.of(
            "image_url", mediaUrl,
            "caption",   caption != null ? caption : "",
            "access_token", accessToken
        );

        try {
            var raw = RestClient.create().post()
                .uri(GRAPH_URL + "/" + API_VERSION + "/" + igUserId + "/media")
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(raw);
            if (node.has("error")) {
                throw new RuntimeException("Meta API error: " + node.path("error").path("message").asText());
            }
            return node.path("id").asText();
        } catch (Exception e) {
            log.error("Error creant container IG per {}: {}", igUserId, e.getMessage());
            throw new RuntimeException("Error creant container Instagram: " + e.getMessage(), e);
        }
    }

    private String publishContainer(String igUserId, String accessToken, String containerId) {
        var body = Map.of(
            "creation_id", containerId,
            "access_token", accessToken
        );

        try {
            var raw = RestClient.create().post()
                .uri(GRAPH_URL + "/" + API_VERSION + "/" + igUserId + "/media_publish")
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(raw);
            if (node.has("error")) {
                throw new RuntimeException("Meta API error: " + node.path("error").path("message").asText());
            }
            return node.path("id").asText();
        } catch (Exception e) {
            log.error("Error publicant container IG {} per {}: {}", containerId, igUserId, e.getMessage());
            throw new RuntimeException("Error publicant a Instagram: " + e.getMessage(), e);
        }
    }
}
