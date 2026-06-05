package com.amg.digitalitzacio.metaads.application;

import com.amg.digitalitzacio.metaads.api.dto.ImageUploadResponse;
import com.amg.digitalitzacio.metaads.api.dto.TargetingSearchResult;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaAdsTargetingService {

    private static final String GRAPH_BASE = "https://graph.facebook.com";
    private static final String API_VERSION = "v19.0";

    private final MetaAdsConfigRepository configRepository;
    private final SystemConfigService sysConfig;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public List<TargetingSearchResult.Item> searchInterests(String query, UUID tenantId) {
        String token = resolveToken(tenantId);
        if (token == null) return List.of();
        try {
            var rc = restClientBuilder.baseUrl(GRAPH_BASE).build();
            String raw = rc.get()
                    .uri("/" + API_VERSION + "/search?type=adinterest&q={q}&access_token={t}",
                            query, token)
                    .retrieve().body(String.class);
            return parseItems(raw, "audience_size");
        } catch (Exception e) {
            log.warn("[MetaAds] Error cercant interessos '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    public List<TargetingSearchResult.Item> searchLocations(String query, UUID tenantId) {
        String token = resolveToken(tenantId);
        if (token == null) return List.of();
        try {
            var rc = restClientBuilder.baseUrl(GRAPH_BASE).build();
            String raw = rc.get()
                    .uri("/" + API_VERSION + "/search?type=adgeolocation&q={q}&location_types=[\"city\"]&access_token={t}",
                            query, token)
                    .retrieve().body(String.class);
            return parseItems(raw, "country_code");
        } catch (Exception e) {
            log.warn("[MetaAds] Error cercant localitzacions '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    public ImageUploadResponse uploadImage(UUID tenantId, MultipartFile file) {
        String token = resolveToken(tenantId);
        if (token == null) throw new IllegalStateException("Token Meta Ads no configurat");

        var config = configRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Meta Ads no configurat"));
        String accountId = normalizeAccountId(config.getAdAccountId());

        try {
            var form = new LinkedMultiValueMap<String, Object>();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            form.add(filename, new ByteArrayResource(file.getBytes()) {
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
            if (first == null) throw new RuntimeException("Resposta inesperada de Meta al pujar imatge");

            return new ImageUploadResponse(first.path("hash").asText(), first.path("url").asText());

        } catch (Exception e) {
            log.error("[MetaAds] Error pujant imatge al tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException("Error pujant imatge a Meta: " + e.getMessage());
        }
    }

    private List<TargetingSearchResult.Item> parseItems(String raw, String extraField) throws Exception {
        JsonNode data = objectMapper.readTree(raw).path("data");
        List<TargetingSearchResult.Item> items = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode n : data) {
                long size = n.has("audience_size") ? n.path("audience_size").asLong(0) : 0;
                items.add(new TargetingSearchResult.Item(
                    n.path("id").asText(),
                    n.path("name").asText(),
                    size > 0 ? size : null
                ));
            }
        }
        return items;
    }

    private String resolveToken(UUID tenantId) {
        if (tenantId != null) {
            var config = configRepository.findById(tenantId).orElse(null);
            if (config != null && config.getAccessToken() != null && !config.getAccessToken().isBlank())
                return config.getAccessToken();
        }
        return sysConfig.get("META_ADS_MANAGEMENT_TOKEN");
    }

    private String normalizeAccountId(String id) {
        if (id == null) return null;
        return id.startsWith("act_") ? id : "act_" + id;
    }
}
