package com.amg.digitalitzacio.social.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Publica posts i fotos a Facebook Page via Meta Graph API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookPublisherService {

    private static final String GRAPH_URL   = "https://graph.facebook.com";
    private static final String API_VERSION = "v22.0";

    private final ObjectMapper objectMapper;

    /**
     * Publica text a la pàgina de Facebook.
     * @return ID extern del post publicat
     */
    public String publishText(String pageId, String accessToken, String message) {
        var body = Map.of(
            "message",      message,
            "access_token", accessToken
        );
        return postToFeed(pageId, "/feed", body);
    }

    /**
     * Publica foto amb caption a la pàgina de Facebook.
     * @return ID extern del post publicat
     */
    public String publishPhoto(String pageId, String accessToken, String photoUrl, String caption) {
        var body = new HashMap<String, Object>();
        body.put("url",          photoUrl);
        body.put("caption",      caption != null ? caption : "");
        body.put("access_token", accessToken);
        return postToFeed(pageId, "/photos", body);
    }

    private String postToFeed(String pageId, String path, Map<String, ?> body) {
        try {
            var raw = RestClient.create().post()
                .uri(GRAPH_URL + "/" + API_VERSION + "/" + pageId + path)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(raw);
            if (node.has("error")) {
                throw new RuntimeException("Meta API error: " + node.path("error").path("message").asText());
            }
            return node.has("post_id") ? node.path("post_id").asText() : node.path("id").asText();
        } catch (Exception e) {
            log.error("Error publicant a Facebook page {}: {}", pageId, e.getMessage());
            throw new RuntimeException("Error publicant a Facebook: " + e.getMessage(), e);
        }
    }
}
