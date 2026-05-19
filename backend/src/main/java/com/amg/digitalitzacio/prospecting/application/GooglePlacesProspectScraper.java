package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;
import com.amg.digitalitzacio.prospecting.domain.ProspectSource;
import com.amg.digitalitzacio.prospecting.domain.ProspectStatus;
import com.amg.digitalitzacio.shared.exception.MissingApiKeyException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.prospecting.provider", havingValue = "google-places")
@Slf4j
public class GooglePlacesProspectScraper implements ProspectScraper {

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SystemConfigService sysConfig;

    @Value("${app.prospecting.google.rate-limit-per-second:10}")
    private int rateLimitPerSecond;

    private long lastRequestTime = 0;

    public GooglePlacesProspectScraper(SystemConfigService sysConfig) {
        this.webClient = WebClient.builder().baseUrl(PLACES_API_BASE).build();
        this.objectMapper = new ObjectMapper();
        this.sysConfig = sysConfig;
    }

    @PostConstruct
    void init() {
        if (!sysConfig.isConfigured("GOOGLE_PLACES_API_KEY")) {
            log.warn("GooglePlacesProspectScraper: GOOGLE_PLACES_API_KEY no configurada — configura-la a /portal/admin/config");
        } else {
            log.info("GooglePlacesProspectScraper inicialitzat (rate limit: {}/s)", rateLimitPerSecond);
        }
    }

    @Override
    public List<Prospect> search(String sector, String location, Map<String, Object> params, UUID campaignId) {
        String apiKey = sysConfig.get("GOOGLE_PLACES_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new MissingApiKeyException("Google Places", "GOOGLE_PLACES_API_KEY");
        }

        var query = sector + " a " + location + ", Mallorca";
        var maxResults = params != null && params.get("maxResults") instanceof Number n
                ? n.intValue() : 50;

        log.info("Searching Google Places: query='{}', maxResults={}", query, maxResults);

        var prospects = new ArrayList<Prospect>();
        String nextPageToken = null;

        try {
            do {
                throttle();

                var uri = "/textsearch/json?query=" + encode(query) + "&key=" + apiKey
                        + (nextPageToken != null ? "&pagetoken=" + nextPageToken : "");

                var response = webClient.get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                if (response == null) break;

                var status = response.path("status").asText();
                if ("OVER_QUERY_LIMIT".equals(status)) {
                    log.warn("Google Places rate limit exceeded, waiting before retry");
                    Thread.sleep(2000);
                    continue;
                }
                if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                    log.warn("Google Places API error: status={}, error={}", status,
                            response.path("error_message").asText());
                    break;
                }

                var results = response.path("results");
                for (var result : results) {
                    if (prospects.size() >= maxResults) break;

                    var placeId = result.path("place_id").asText();
                    var name = result.path("name").asText();
                    var address = result.path("formatted_address").asText("");
                    var rating = result.path("rating");
                    var reviews = result.path("user_ratings_total");
                    var hasWebsite = result.has("website");

                    var prospect = Prospect.builder()
                            .campaignId(campaignId)
                            .name(name)
                            .sector(sector)
                            .address(address)
                            .city(extractCity(address, location))
                            .googleRating(rating.isNumber() ? BigDecimal.valueOf(rating.doubleValue()) : null)
                            .googleReviews(reviews.isInt() ? reviews.asInt() : null)
                            .googlePlaceId(placeId)
                            .hasWebsite(hasWebsite)
                            .status(ProspectStatus.NEW)
                            .source(ProspectSource.GOOGLE_MAPS)
                            .build();

                    // Enriquir amb Place Details per obtenir telèfon i web
                    enrichFromDetails(prospect, apiKey);

                    prospects.add(prospect);
                }

                nextPageToken = response.path("next_page_token").asText(null);
                if (nextPageToken != null) {
                    Thread.sleep(100); // Google requereix un petit delay abans d'usar el token
                }
            } while (nextPageToken != null && prospects.size() < maxResults);

            log.info("Google Places search found {} prospects", prospects.size());
        } catch (Exception e) {
            log.error("Google Places search failed: {}", e.getMessage());
        }

        return prospects;
    }

    @Override
    public ProspectEnrichment enrich(String name, String website) {
        if (website == null || website.isBlank()) {
            return new ProspectEnrichment(null, null, false, false);
        }
        var domain = website.replace("https://", "").replace("http://", "").split("/")[0];
        return new ProspectEnrichment(
                "contact@" + domain,
                "@" + name.toLowerCase().replaceAll("\\s+", ""),
                true,
                false
        );
    }

    private void enrichFromDetails(Prospect prospect, String apiKey) {
        try {
            throttle();

            var uri = "/details/json?place_id=" + prospect.getGooglePlaceId()
                    + "&fields=name,formatted_address,formatted_phone_number,website,rating,user_ratings_total"
                    + "&key=" + apiKey;

            var response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return;

            var result = response.path("result");
            if (result.isMissingNode()) return;

            var phone = result.path("formatted_phone_number").asText(null);
            if (phone != null && !phone.isBlank()) {
                prospect.setPhone(phone);
            }

            var website = result.path("website").asText(null);
            if (website != null && !website.isBlank()) {
                prospect.setWebsite(website);
                prospect.setHasWebsite(true);
            }
        } catch (Exception e) {
            log.debug("Failed to enrich details for place {}: {}", prospect.getGooglePlaceId(), e.getMessage());
        }
    }

    private void throttle() throws InterruptedException {
        if (rateLimitPerSecond <= 0) return;
        var minInterval = 1000 / rateLimitPerSecond;
        var now = System.currentTimeMillis();
        var elapsed = now - lastRequestTime;
        if (elapsed < minInterval) {
            Thread.sleep(minInterval - elapsed);
        }
        lastRequestTime = System.currentTimeMillis();
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private String extractCity(String address, String defaultCity) {
        if (address == null || address.isBlank()) return defaultCity;
        var parts = address.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            var part = parts[i].trim();
            if (part.contains(" ") && !part.matches("\\d+.*")) {
                return part;
            }
        }
        return defaultCity;
    }
}
