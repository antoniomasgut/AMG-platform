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
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Value("${GOOGLE_PLACES_API_KEY:}")
    private String apiKeyFromEnv;

    private long lastRequestTime = 0;

    public GooglePlacesProspectScraper(SystemConfigService sysConfig) {
        this.webClient = WebClient.builder().baseUrl(PLACES_API_BASE).build();
        this.objectMapper = new ObjectMapper();
        this.sysConfig = sysConfig;
    }

    @PostConstruct
    void init() {
        if (!resolvedApiKey().isBlank()) {
            log.info("GooglePlacesProspectScraper inicialitzat (rate limit: {}/s)", rateLimitPerSecond);
        } else {
            log.warn("GooglePlacesProspectScraper: GOOGLE_PLACES_API_KEY no configurada — afegeix-la a .env o a /portal/admin/config");
        }
    }

    private String resolvedApiKey() {
        if (apiKeyFromEnv != null && !apiKeyFromEnv.isBlank()) return apiKeyFromEnv;
        var fromDb = sysConfig.get("GOOGLE_PLACES_API_KEY");
        return fromDb != null ? fromDb : "";
    }

    @Override
    public List<Prospect> search(String sector, String location, Map<String, Object> params, UUID campaignId) {
        String apiKey = resolvedApiKey();
        if (apiKey.isBlank()) {
            throw new MissingApiKeyException("Google Places", "GOOGLE_PLACES_API_KEY");
        }

        var maxResults = params != null && params.get("maxResults") instanceof Number n
                ? n.intValue() : 20;

        // Construir llista de queries: sector principal + keywords addicionals
        var queries = new java.util.LinkedHashSet<String>();
        queries.add(sector + " a " + location);
        if (params != null && params.get("keywords") instanceof java.util.List<?> kws) {
            for (var kw : kws) {
                if (kw instanceof String s && !s.isBlank()) {
                    queries.add(s.trim() + " a " + location);
                }
            }
        }

        log.info("Searching Google Places: queries={}, maxResults={}", queries, maxResults);

        // Cercar per totes les queries i fusionar resultats (deduplicats per placeId)
        var seen = new java.util.LinkedHashSet<String>();
        var allProspects = new ArrayList<Prospect>();
        for (var query : queries) {
            if (allProspects.size() >= maxResults) break;
            var partial = searchQuery(query, sector, location, apiKey, maxResults - allProspects.size(), campaignId);
            for (var p : partial) {
                if (p.getGooglePlaceId() != null && seen.add(p.getGooglePlaceId())) {
                    allProspects.add(p);
                }
            }
        }
        log.info("Google Places total found: {} prospects from {} queries", allProspects.size(), queries.size());
        return allProspects;
    }

    private List<Prospect> searchQuery(String query, String sector, String location, String apiKey, int maxResults, UUID campaignId) {
        var prospects = new ArrayList<Prospect>();
        String nextPageToken = null;

        log.info("Searching Google Places: query='{}', maxResults={}", query, maxResults);

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

    private static final Set<String> GENERIC_DOMAINS = Set.of(
        "gmail.com", "hotmail.com", "yahoo.com", "outlook.com",
        "icloud.com", "live.com", "msn.com", "yahoo.es", "hotmail.es",
        "googlemail.com", "me.com"
    );

    @Override
    public ProspectEnrichment enrich(String name, String existingWebsite, String email) {
        String resolvedWebsite = existingWebsite;

        // Si no té web però té email amb domini propi, prova de trobar la web
        if ((resolvedWebsite == null || resolvedWebsite.isBlank())
                && email != null && !email.isBlank()) {
            resolvedWebsite = findWebsiteFromEmailDomain(email);
        }

        // Si tenim web, intentar extreure email
        String resolvedEmail = null;
        if (resolvedWebsite != null && !resolvedWebsite.isBlank()) {
            resolvedEmail = extractEmailFromWebsite(resolvedWebsite);
        }

        return new ProspectEnrichment(resolvedEmail, resolvedWebsite, null,
                resolvedWebsite != null, false);
    }

    private String findWebsiteFromEmailDomain(String email) {
        var parts = email.split("@");
        if (parts.length != 2) return null;
        var domain = parts[1].toLowerCase().trim();
        if (GENERIC_DOMAINS.contains(domain)) return null;

        var httpClient = buildScrapingClient();

        for (var candidate : List.of("https://" + domain, "https://www." + domain)) {
            try {
                var response = httpClient.get()
                    .uri(candidate)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Found website {} from email domain {}", candidate, domain);
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Override
    public PlaceDetails fetchDetails(String googlePlaceId) {
        String apiKey = resolvedApiKey();
        if (apiKey.isBlank() || googlePlaceId == null) return new PlaceDetails(null, null, null, null, null);
        try {
            throttle();
            var uri = "/details/json?place_id=" + googlePlaceId
                    + "&fields=formatted_phone_number,website,editorial_summary,address_components"
                    + "&key=" + apiKey;
            var response = webClient.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block();
            if (response == null) return new PlaceDetails(null, null, null, null, null);
            var result = response.path("result");
            if (result.isMissingNode()) return new PlaceDetails(null, null, null, null, null);

            var phone = result.path("formatted_phone_number").asText(null);
            var website = result.path("website").asText(null);
            var description = result.path("editorial_summary").path("overview").asText(null);
            String city = null, postalCode = null;

            var components = result.path("address_components");
            if (components.isArray()) {
                for (var comp : components) {
                    var longName = comp.path("long_name").asText(null);
                    if (longName == null) continue;
                    for (var type : comp.path("types")) {
                        switch (type.asText()) {
                            case "locality", "administrative_area_level_2" -> { if (city == null) city = longName; }
                            case "postal_code" -> postalCode = longName;
                        }
                    }
                }
            }

            return new PlaceDetails(
                phone != null && !phone.isBlank() ? phone : null,
                website != null && !website.isBlank() ? website : null,
                city, postalCode,
                description != null && !description.isBlank() ? description : null
            );
        } catch (Exception e) {
            log.debug("fetchDetails failed for {}: {}", googlePlaceId, e.getMessage());
            return new PlaceDetails(null, null, null, null, null);
        }
    }

    /** WebClient per scraping de webs externes — ignora errors SSL de certs mal configurats. */
    private WebClient buildScrapingClient() {
        try {
            var sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
            var httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext))
                .followRedirect(true);
            return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
        } catch (Exception e) {
            return WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
        }
    }

    /** Cerca el primer mailto: a la pàgina principal i pàgines de contacte de la web del negoci. */
    private String extractEmailFromWebsite(String websiteUrl) {
        var emailPattern = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
        );
        var mailtoPattern = java.util.regex.Pattern.compile("mailto:([^\"'?\\s>&]+)");

        var paths = List.of(
            "", "/contacte", "/contact", "/contacto",
            "/sobre-nosaltres", "/sobre-nosotros", "/quienes-somos",
            "/nosotros", "/about", "/qui-som"
        );
        var base = websiteUrl.replaceAll("/$", "");
        var httpClient = buildScrapingClient();

        for (var path : paths) {
            try {
                var html = httpClient.get()
                    .uri(base + path)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(7))
                    .block();

                if (html == null) continue;

                // 1. Buscar mailto: links (més fiable)
                var m = mailtoPattern.matcher(html);
                while (m.find()) {
                    var email = m.group(1).toLowerCase().trim();
                    if (!isSpamEmail(email)) return email;
                }

                // 2. Buscar emails en text pla
                var em = emailPattern.matcher(html);
                while (em.find()) {
                    var email = em.group().toLowerCase();
                    if (!isSpamEmail(email)) return email;
                }
            } catch (Exception ignored) {}
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
