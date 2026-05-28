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
                    + "&fields=formatted_phone_number,website,editorial_summary,address_components,formatted_address"
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

            // Adreça completa i components estructurats
            var formattedAddress = result.path("formatted_address").asText(null);
            if (formattedAddress != null && !formattedAddress.isBlank()) {
                prospect.setAddress(formattedAddress);
            }
            parseAddressComponents(result.path("address_components"), prospect);

            var website = result.path("website").asText(null);
            if (website != null && !website.isBlank()) {
                prospect.setWebsite(website);
                prospect.setHasWebsite(true);
                // Intentar extreure email de la web del negoci
                var email = extractEmailFromWebsite(website);
                if (email != null) prospect.setEmail(email);
            }

            // Descripció editorial (no sempre disponible)
            var summary = result.path("editorial_summary").path("overview").asText(null);
            if (summary != null && !summary.isBlank()) {
                prospect.setDescription(summary);
            }

        } catch (Exception e) {
            log.debug("Failed to enrich details for place {}: {}", prospect.getGooglePlaceId(), e.getMessage());
        }
    }

    /** Extreu ciutat i codi postal dels address_components de Google Places. */
    private void parseAddressComponents(JsonNode components, Prospect prospect) {
        if (components == null || !components.isArray()) return;
        for (var comp : components) {
            var types = comp.path("types");
            var longName = comp.path("long_name").asText(null);
            if (longName == null) continue;
            for (var type : types) {
                switch (type.asText()) {
                    case "locality", "administrative_area_level_2" -> {
                        if (prospect.getCity() == null || prospect.getCity().isBlank()) {
                            prospect.setCity(longName);
                        }
                    }
                    case "postal_code" -> prospect.setPostalCode(longName);
                }
            }
        }
    }

    /** Cerca el primer mailto: a la pàgina principal i /contacte de la web del negoci. */
    private String extractEmailFromWebsite(String websiteUrl) {
        var emailPattern = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
        );
        // Pàgines de contacte habituals
        var paths = List.of("", "/contacte", "/contact", "/contacto", "/sobre-nosaltres");
        var base = websiteUrl.replaceAll("/$", "");

        var httpClient = WebClient.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
            .build();

        for (var path : paths) {
            try {
                var html = httpClient.get()
                    .uri(base + path)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(4))
                    .block();

                if (html == null) continue;

                // Buscar mailto: links primer (més fiable)
                var mailtoPattern = java.util.regex.Pattern.compile("mailto:([^\"'?\\s>]+)");
                var m = mailtoPattern.matcher(html);
                if (m.find()) {
                    var email = m.group(1).toLowerCase();
                    if (!isSpamEmail(email)) return email;
                }

                // Buscar emails en text pla
                var em = emailPattern.matcher(html);
                while (em.find()) {
                    var email = em.group().toLowerCase();
                    if (!isSpamEmail(email)) return email;
                }
            } catch (Exception ignored) {
                // Timeout o web no accessible — continuar amb el següent path
            }
        }
        return null;
    }

    private boolean isSpamEmail(String email) {
        // Filtrar emails genèrics de plataformes, imatges, scripts, etc.
        return email.contains("sentry") || email.contains("example")
            || email.contains("@2x") || email.contains(".png") || email.contains(".jpg")
            || email.contains("noreply") || email.contains("no-reply")
            || email.contains("wordpress") || email.contains("schema.org")
            || email.contains("@w3") || email.contains("wixpress")
            || email.length() > 80;
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
