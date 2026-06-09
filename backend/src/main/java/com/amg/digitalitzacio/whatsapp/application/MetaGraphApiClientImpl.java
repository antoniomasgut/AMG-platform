package com.amg.digitalitzacio.whatsapp.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class MetaGraphApiClientImpl implements MetaGraphApiClient {

    private static final String GRAPH_URL  = "https://graph.facebook.com";
    private static final String API_VERSION = "v22.0";

    private final SystemConfigService sysConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String exchangeCodeForToken(String code, String redirectUri) {
        String appId     = require("META_APP_ID");
        String appSecret = require("META_APP_SECRET");

        var raw = RestClient.create().get()
            .uri(GRAPH_URL + "/" + API_VERSION + "/oauth/access_token"
                + "?client_id=" + appId
                + "&client_secret=" + appSecret
                + "&redirect_uri=" + encode(redirectUri)
                + "&code=" + code)
            .retrieve()
            .body(String.class);

        return parseField(raw, "access_token");
    }

    @Override
    public String getUserId(String accessToken) {
        var raw = RestClient.create().get()
            .uri(GRAPH_URL + "/" + API_VERSION + "/me?fields=id&access_token=" + accessToken)
            .retrieve()
            .body(String.class);
        return parseField(raw, "id");
    }

    @Override
    public List<WabaOption> listWabas(String accessToken) {
        // Obté els negocis de l'usuari i per cada negoci els WABAs
        var raw = RestClient.create().get()
            .uri(GRAPH_URL + "/" + API_VERSION
                + "/me/businesses?fields=id,name,owned_whatsapp_business_accounts{id,name}&access_token=" + accessToken)
            .retrieve()
            .body(String.class);

        var wabas = new ArrayList<WabaOption>();
        try {
            var root = objectMapper.readTree(raw);
            for (JsonNode biz : root.path("data")) {
                for (JsonNode waba : biz.path("owned_whatsapp_business_accounts").path("data")) {
                    wabas.add(new WabaOption(
                        waba.path("id").asText(),
                        waba.path("name").asText("")
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing WABA list: {}", e.getMessage());
        }
        return wabas;
    }

    @Override
    public List<PhoneOption> listPhoneNumbers(String accessToken, String wabaId) {
        var raw = RestClient.create().get()
            .uri(GRAPH_URL + "/" + API_VERSION
                + "/" + wabaId + "/phone_numbers?fields=id,display_phone_number,verified_name&access_token=" + accessToken)
            .retrieve()
            .body(String.class);

        var phones = new ArrayList<PhoneOption>();
        try {
            var root = objectMapper.readTree(raw);
            for (JsonNode p : root.path("data")) {
                phones.add(new PhoneOption(
                    p.path("id").asText(),
                    p.path("display_phone_number").asText(""),
                    p.path("verified_name").asText("")
                ));
            }
        } catch (Exception e) {
            log.warn("Error parsing phone number list: {}", e.getMessage());
        }
        return phones;
    }

    @Override
    public void subscribeToWebhook(String systemUserToken, String wabaId) {
        RestClient.create().post()
            .uri(GRAPH_URL + "/" + API_VERSION + "/" + wabaId + "/subscribed_apps")
            .header("Authorization", "Bearer " + systemUserToken)
            .body(Map.of())
            .retrieve()
            .toBodilessEntity();
        log.info("Webhook subscribed for wabaId={}", wabaId);
    }

    @Override
    public void revokePermissions(String accessToken, String metaUserId) {
        String appId = require("META_APP_ID");
        try {
            RestClient.create().delete()
                .uri(GRAPH_URL + "/" + API_VERSION + "/" + metaUserId + "/permissions"
                    + "?access_token=" + accessToken
                    + "&permission=whatsapp_business_management,whatsapp_business_messaging")
                .retrieve()
                .toBodilessEntity();
            log.info("Meta permissions revoked for user {}", metaUserId);
        } catch (Exception e) {
            log.warn("Could not revoke Meta permissions for user {}: {}", metaUserId, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String require(String key) {
        String v = sysConfig.get(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Configuració Meta no trobada: " + key);
        return v;
    }

    private String parseField(String json, String field) {
        try {
            return objectMapper.readTree(json).path(field).asText(null);
        } catch (Exception e) {
            throw new RuntimeException("Error parsejant resposta Meta: " + e.getMessage());
        }
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
