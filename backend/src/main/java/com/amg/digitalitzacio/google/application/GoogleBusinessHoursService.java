package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.google.domain.GoogleModuleConfig;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publica dies tancats (festius i absències F2) com a "special hours" al perfil
 * de Google Business del tenant (Business Information API v1) — Mòdul 57 F1.
 * No bloquejant: qualsevol error es loga i retorna false.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleBusinessHoursService {

    private static final String GBI_BASE = "https://mybusinessbusinessinformation.googleapis.com/v1";

    private final GoogleModuleConfigRepository configRepo;
    private final GoogleTokenService tokenService;
    private final ObjectMapper objectMapper;

    /**
     * Marca com a tancat cada dia del rang [start, end] als special hours del perfil.
     * Retorna true si s'ha publicat a Google; false si el tenant no té GBP configurat o hi ha error.
     */
    public boolean markClosed(UUID tenantId, LocalDate start, LocalDate end) {
        var config = configRepo.findById(tenantId)
            .filter(GoogleModuleConfig::isBusinessEnabled)
            .filter(c -> c.getBusinessLocationId() != null && !c.getBusinessLocationId().isBlank())
            .orElse(null);
        if (config == null) return false;
        if (start == null || end == null || end.isBefore(start)) return false;

        try {
            var creds = tokenService.getValidCredentials(tenantId);
            var location = normalizeLocation(config.getBusinessLocationId());
            var client = WebClient.builder().baseUrl(GBI_BASE).build();

            var current = client.get()
                .uri("/{loc}?readMask=specialHours", location)
                .header("Authorization", "Bearer " + creds.accessToken())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

            var merged = mergeClosedPeriods(
                current != null ? objectMapper.readTree(current) : null,
                start, end, LocalDate.now());

            client.patch()
                .uri("/{loc}?updateMask=specialHours", location)
                .header("Authorization", "Bearer " + creds.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("specialHours", Map.of("specialHourPeriods", merged)))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

            log.info("GBP special hours: {} dia(es) tancat(s) publicats per tenant {}", merged.size(), tenantId);
            return true;
        } catch (Exception e) {
            log.warn("GBP special hours error tenant {}: {}", tenantId, e.getMessage());
            return false;
        }
    }

    /**
     * Fusiona els períodes existents amb el rang nou de dies tancats:
     * descarta períodes passats, conserva els futurs que no coincideixin amb el rang
     * i afegeix un període "closed" per cada dia del rang (sense duplicats).
     */
    List<Map<String, Object>> mergeClosedPeriods(JsonNode currentResponse, LocalDate start,
                                                 LocalDate end, LocalDate today) {
        var result = new ArrayList<Map<String, Object>>();

        if (currentResponse != null) {
            var periods = currentResponse.path("specialHours").path("specialHourPeriods");
            if (periods.isArray()) {
                for (JsonNode p : periods) {
                    var date = parseDate(p.path("startDate"));
                    if (date == null || date.isBefore(today)) continue;
                    if (!date.isBefore(start) && !date.isAfter(end)) continue;
                    result.add(objectMapper.convertValue(p, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {}));
                }
            }
        }

        for (var d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.isBefore(today)) continue;
            result.add(Map.of(
                "startDate", Map.of("year", d.getYear(), "month", d.getMonthValue(), "day", d.getDayOfMonth()),
                "closed", true));
        }
        return result;
    }

    private LocalDate parseDate(JsonNode dateNode) {
        int year = dateNode.path("year").asInt(0);
        int month = dateNode.path("month").asInt(0);
        int day = dateNode.path("day").asInt(0);
        if (year == 0 || month == 0 || day == 0) return null;
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /** Accepta un id cru, "locations/123" o "accounts/x/locations/123" i retorna "locations/123" */
    private String normalizeLocation(String raw) {
        var v = raw.trim();
        int idx = v.lastIndexOf("locations/");
        if (idx >= 0) return v.substring(idx);
        return "locations/" + v;
    }
}
