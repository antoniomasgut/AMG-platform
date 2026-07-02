package com.amg.digitalitzacio.domains.application;

import com.amg.digitalitzacio.domains.domain.DomainDnsRecord;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

// Client OpenProvider per a registre i gestió de dominis.
// Les credencials es llegeixen de SystemConfigService (BD xifrada o env var).
// Si no estan configurades, els mètodes de lectura retornen valors neutres
// i els mètodes d'escriptura llancen IllegalStateException.
@Slf4j
@Component
@Profile("!test")   // en tests s'usa MockDomainRegistrarClient
@RequiredArgsConstructor
public class OpenProviderDomainClient implements DomainRegistrarClient {

    private static final String BASE_URL = "https://api.openprovider.eu/v1beta";

    private final SystemConfigService systemConfig;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final AtomicReference<String> cachedToken = new AtomicReference<>();

    public boolean isConfigured() {
        var u = systemConfig.get("OPENPROVIDER_USERNAME");
        var p = systemConfig.get("OPENPROVIDER_PASSWORD");
        return u != null && !u.isBlank() && p != null && !p.isBlank();
    }

    @Override
    public boolean checkAvailability(String domainName) {
        if (!isConfigured()) {
            log.warn("[OP] Credencials OpenProvider no configurades — checkAvailability retorna false per a {}", domainName);
            return false;
        }
        try {
            var parts = domainName.split("\\.", 2);
            if (parts.length < 2) return false;
            var token = getToken();
            var uri = URI.create(BASE_URL + "/domains/check?domain=" + parts[0] + "&extension=" + parts[1]);
            var req = HttpRequest.newBuilder(uri).GET()
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15)).build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            var status = objectMapper.readTree(resp.body()).path("data").path("status").asText();
            log.info("[OP] checkAvailability: {} -> {}", domainName, status);
            return "free".equalsIgnoreCase(status);
        } catch (Exception e) {
            cachedToken.set(null); // invalida token per al pròxim intent
            log.error("[OP] Error comprovant {}: {}", domainName, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> suggestAlternatives(String domainName) {
        if (!isConfigured()) return List.of();
        try {
            var name = domainName.split("\\.")[0];
            return List.of("cat", "es", "com", "net", "org").stream()
                    .map(tld -> name + "." + tld)
                    .filter(alt -> !alt.equals(domainName))
                    .filter(alt -> { try { return checkAvailability(alt); } catch (Exception e) { return false; } })
                    .limit(3)
                    .toList();
        } catch (Exception e) {
            log.error("[OP] Error suggerint alternatives: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String registerDomain(String domainName, String registrantName, String registrantEmail,
                                  String registrantPhone, String registrantNif) {
        requireConfigured();
        try {
            var parts = domainName.split("\\.", 2);
            var token = getToken();
            var body = objectMapper.writeValueAsString(Map.of(
                    "domain", Map.of("name", parts[0], "extension", parts.length > 1 ? parts[1] : ""),
                    "owner_handle", Map.of(
                            "name", registrantName != null ? registrantName : "",
                            "email", registrantEmail != null ? registrantEmail : "",
                            "phone", registrantPhone != null ? registrantPhone : "",
                            "vat", registrantNif != null ? registrantNif : ""),
                    "autorenew", "on",
                    "period", 1
            ));
            var req = HttpRequest.newBuilder(URI.create(BASE_URL + "/domains"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30)).build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            var id = objectMapper.readTree(resp.body()).path("data").path("id").asText();
            log.info("[OP] registerDomain: {} -> id={}", domainName, id);
            return id;
        } catch (Exception e) {
            cachedToken.set(null);
            throw new RuntimeException("Error registrant domini: " + e.getMessage(), e);
        }
    }

    @Override
    public void renewDomain(String providerDomainId) {
        requireConfigured();
        try {
            var token = getToken();
            var body = objectMapper.writeValueAsString(Map.of("period", 1));
            var req = HttpRequest.newBuilder(URI.create(BASE_URL + "/domains/" + providerDomainId + "/renew"))
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30)).build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("[OP] renewDomain: {}", providerDomainId);
        } catch (Exception e) {
            cachedToken.set(null);
            throw new RuntimeException("Error renovant domini: " + e.getMessage(), e);
        }
    }

    @Override
    public void setDnsRecords(String providerDomainId, List<DomainDnsRecord> records) {
        requireConfigured();
        try {
            var token = getToken();
            var opRecords = records.stream()
                    .map(r -> Map.of("type", r.getType(), "name", r.getName(),
                            "value", r.getValue(), "ttl", r.getTtl()))
                    .toList();
            var body = objectMapper.writeValueAsString(Map.of("records", opRecords));
            var req = HttpRequest.newBuilder(URI.create(BASE_URL + "/dns/zones/" + providerDomainId + "/records"))
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30)).build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("[OP] setDnsRecords: {}, {} registres", providerDomainId, records.size());
        } catch (Exception e) {
            cachedToken.set(null);
            throw new RuntimeException("Error configurant DNS: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelDomain(String providerDomainId) {
        requireConfigured();
        try {
            var token = getToken();
            var req = HttpRequest.newBuilder(URI.create(BASE_URL + "/domains/" + providerDomainId))
                    .DELETE()
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(30)).build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("[OP] cancelDomain: {}", providerDomainId);
        } catch (Exception e) {
            cachedToken.set(null);
            throw new RuntimeException("Error cancel·lant domini: " + e.getMessage(), e);
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                "OpenProvider no configurat. Afegeix OPENPROVIDER_USERNAME i OPENPROVIDER_PASSWORD a la configuració del sistema.");
        }
    }

    // Obté o renova el token JWT d'OpenProvider. Invalida la caché si falla.
    private String getToken() throws Exception {
        var existing = cachedToken.get();
        if (existing != null) return existing;

        var username = systemConfig.get("OPENPROVIDER_USERNAME");
        var password = systemConfig.get("OPENPROVIDER_PASSWORD");
        var body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        var req = HttpRequest.newBuilder(URI.create(BASE_URL + "/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10)).build();
        var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        var token = objectMapper.readTree(resp.body()).path("data").path("token").asText();
        if (token == null || token.isBlank()) {
            throw new RuntimeException("OpenProvider no ha retornat token: " + resp.body());
        }
        cachedToken.set(token);
        log.info("[OP] Token obtingut correctament");
        return token;
    }
}
