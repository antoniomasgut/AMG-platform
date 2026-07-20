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
        String containerId = createContainer(igUserId, Map.of(
            "image_url",    mediaUrl,
            "caption",      caption != null ? caption : "",
            "access_token", accessToken));
        return publishContainer(igUserId, accessToken, containerId);
    }

    /**
     * Publica un Reel (vídeo al feed). El processament de vídeo és asíncron a Meta:
     * cal esperar que el contenidor arribi a FINISHED abans de publicar.
     */
    public String publishReel(String igUserId, String accessToken, String videoUrl, String caption) {
        String containerId = createContainer(igUserId, Map.of(
            "media_type",   "REELS",
            "video_url",    videoUrl,
            "caption",      caption != null ? caption : "",
            "access_token", accessToken));
        waitUntilFinished(containerId, accessToken);
        return publishContainer(igUserId, accessToken, containerId);
    }

    /** Publica una Story (foto o vídeo). Les stories no porten caption a l'API. */
    public String publishStory(String igUserId, String accessToken, String mediaUrl, boolean isVideo) {
        var body = new java.util.HashMap<String, Object>();
        body.put("media_type", "STORIES");
        body.put(isVideo ? "video_url" : "image_url", mediaUrl);
        body.put("access_token", accessToken);
        String containerId = createContainer(igUserId, body);
        if (isVideo) waitUntilFinished(containerId, accessToken);
        return publishContainer(igUserId, accessToken, containerId);
    }

    /**
     * Espera que Meta acabi de processar el contenidor de vídeo (fins a ~2 min).
     * status_code: IN_PROGRESS → FINISHED | ERROR | EXPIRED
     */
    private void waitUntilFinished(String containerId, String accessToken) {
        for (int i = 0; i < 24; i++) {
            try {
                var raw = RestClient.create().get()
                    .uri(GRAPH_URL + "/" + API_VERSION + "/" + containerId
                         + "?fields=status_code&access_token=" + accessToken)
                    .retrieve()
                    .body(String.class);
                String status = objectMapper.readTree(raw).path("status_code").asText();
                switch (status) {
                    case "FINISHED" -> { return; }
                    case "ERROR", "EXPIRED" ->
                        throw new RuntimeException("Meta ha rebutjat el vídeo (status " + status
                            + "). Comprova format (MP4/MOV), durada i mida.");
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interromput esperant el processament del vídeo", e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Error consultant l'estat del contenidor: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Meta no ha acabat de processar el vídeo en 2 minuts. Torna-ho a provar.");
    }

    private String createContainer(String igUserId, Map<String, ?> body) {
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
        } catch (RuntimeException e) {
            log.error("Error creant container IG per {}: {}", igUserId, e.getMessage());
            throw e;
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
