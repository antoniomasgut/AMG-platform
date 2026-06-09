package com.amg.digitalitzacio.whatsapp.application;

import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.amg.digitalitzacio.whatsapp.api.dto.MetaOAuthSessionResponse;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppConnectionStatus;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfig;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetaOAuthService {

    private static final String CALLBACK_PATH  = "/oauth/meta/whatsapp/callback";
    private static final String STATE_KEY       = "meta:oauth:state:%s";
    private static final String SESSION_KEY     = "meta:oauth:session:%s";
    private static final int    STATE_TTL_MIN   = 10;
    private static final int    SESSION_TTL_MIN = 5;

    private final MetaGraphApiClient       graphClient;
    private final WhatsAppWabaConfigRepository configRepository;
    private final VaultEncryption          vaultEncryption;
    private final SystemConfigService      sysConfig;
    private final StringRedisTemplate      redis;
    private final ObjectMapper             objectMapper;

    /** Genera l'URL OAuth de Meta i emmagatzema l'state a Redis. */
    public String generateConnectUrl(UUID tenantId) {
        String appId       = requireConfig("META_APP_ID");
        String callbackUri = apiBaseUrl() + CALLBACK_PATH;
        String state       = UUID.randomUUID().toString();

        redis.opsForValue().set(STATE_KEY.formatted(state), tenantId.toString(),
                STATE_TTL_MIN, TimeUnit.MINUTES);

        return "https://www.facebook.com/dialog/oauth"
            + "?client_id=" + appId
            + "&redirect_uri=" + encode(callbackUri)
            + "&scope=whatsapp_business_management,whatsapp_business_messaging,business_management"
            + "&state=" + state
            + "&response_type=code";
    }

    /**
     * Processa el callback de Meta: valida l'state, intercanvia el codi pel token,
     * obté WABAs i números. Desa el resultat a Redis i retorna el sessionToken.
     */
    public String handleCallback(String code, String state) {
        String tenantIdStr = redis.opsForValue().get(STATE_KEY.formatted(state));
        if (tenantIdStr == null)
            throw new IllegalStateException("State OAuth caducat o invàlid");
        redis.delete(STATE_KEY.formatted(state));

        UUID tenantId = UUID.fromString(tenantIdStr);
        String callbackUri = apiBaseUrl() + CALLBACK_PATH;

        String accessToken = graphClient.exchangeCodeForToken(code, callbackUri);
        String metaUserId  = graphClient.getUserId(accessToken);
        var wabas          = graphClient.listWabas(accessToken);

        // Carrega els números per a cada WABA (màx 10 WABAs per no sobrecarregar)
        var phonesPerWaba = new java.util.LinkedHashMap<String, List<MetaGraphApiClient.PhoneOption>>();
        for (var waba : wabas.stream().limit(10).toList()) {
            try {
                phonesPerWaba.put(waba.wabaId(), graphClient.listPhoneNumbers(accessToken, waba.wabaId()));
            } catch (Exception e) {
                log.warn("No s'han pogut carregar números per WABA {}: {}", waba.wabaId(), e.getMessage());
            }
        }

        String sessionToken = UUID.randomUUID().toString();
        var sessionData = Map.of(
            "tenantId",    tenantId.toString(),
            "accessToken", accessToken,
            "metaUserId",  metaUserId != null ? metaUserId : "",
            "wabas",       wabas,
            "phones",      phonesPerWaba
        );

        try {
            redis.opsForValue().set(SESSION_KEY.formatted(sessionToken),
                    objectMapper.writeValueAsString(sessionData),
                    SESSION_TTL_MIN, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Error desant sessió OAuth: " + e.getMessage());
        }

        log.info("Meta OAuth callback OK per tenant {} — {} WABAs trobats", tenantId, wabas.size());
        return sessionToken;
    }

    /** Llegeix el resultat de la sessió OAuth per mostrar-lo al frontend. */
    public MetaOAuthSessionResponse getSession(String sessionToken) {
        String json = redis.opsForValue().get(SESSION_KEY.formatted(sessionToken));
        if (json == null) throw new ResourceNotFoundException("Sessió OAuth no trobada o caducada");

        try {
            var data  = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            var wabasRaw = (List<Map<String, Object>>) data.get("wabas");
            @SuppressWarnings("unchecked")
            var phonesRaw = (Map<String, List<Map<String, Object>>>) data.get("phones");

            var wabas = wabasRaw.stream()
                .map(w -> new MetaOAuthSessionResponse.WabaChoice(
                    (String) w.get("wabaId"),
                    (String) w.get("businessName"),
                    phonesRaw.getOrDefault((String) w.get("wabaId"), List.of()).stream()
                        .map(p -> new MetaOAuthSessionResponse.PhoneChoice(
                            (String) p.get("phoneNumberId"),
                            (String) p.get("displayPhone"),
                            (String) p.get("verifiedName")))
                        .toList()))
                .toList();

            return new MetaOAuthSessionResponse(sessionToken, wabas);
        } catch (Exception e) {
            throw new RuntimeException("Error parsejant sessió OAuth: " + e.getMessage());
        }
    }

    /** Confirma la selecció de WABA + número i guarda la configuració. */
    @Transactional
    public void confirmConnection(String sessionToken, String wabaId, String phoneNumberId) {
        String json = redis.opsForValue().get(SESSION_KEY.formatted(sessionToken));
        if (json == null) throw new ResourceNotFoundException("Sessió OAuth no trobada o caducada");

        try {
            var data  = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            UUID   tenantId    = UUID.fromString((String) data.get("tenantId"));
            String accessToken = (String) data.get("accessToken");
            String metaUserId  = (String) data.get("metaUserId");

            // Subscriu el WABA al webhook global (usa System User Token si disponible)
            String systemToken = sysConfig.get("META_SYSTEM_USER_TOKEN");
            boolean webhookOk = false;
            if (systemToken != null && !systemToken.isBlank()) {
                try {
                    graphClient.subscribeToWebhook(systemToken, wabaId);
                    webhookOk = true;
                } catch (Exception e) {
                    log.warn("No s'ha pogut subscriure el webhook per wabaId={}: {}", wabaId, e.getMessage());
                }
            }

            // Obté el displayPhone del context de la sessió
            String displayPhone = extractDisplayPhone(data, wabaId, phoneNumberId);

            var config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> WhatsAppWabaConfig.builder().tenantId(tenantId).build());

            config.setWabaId(wabaId);
            config.setPhoneNumberId(phoneNumberId);
            config.setAccessTokenEncrypted(vaultEncryption.encrypt(accessToken));
            config.setMetaUserId(metaUserId);
            config.setDisplayPhoneNumber(displayPhone);
            config.setStatus(WhatsAppConnectionStatus.CONNECTED);
            config.setWebhookRegistered(webhookOk);
            config.setConnectedAt(Instant.now());
            configRepository.save(config);

            redis.delete(SESSION_KEY.formatted(sessionToken));
            log.info("Meta WABA connectat per tenant {} (phoneNumberId={})", tenantId, phoneNumberId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error confirmant connexió Meta: " + e.getMessage());
        }
    }

    /** Revoca els permisos i neteja la configuració. */
    @Transactional
    public void disconnect(UUID tenantId) {
        var config = configRepository.findByTenantId(tenantId).orElse(null);
        if (config == null) return;

        if (config.getAccessTokenEncrypted() != null && config.getMetaUserId() != null
                && !config.getMetaUserId().isBlank()) {
            try {
                String token = vaultEncryption.decrypt(config.getAccessTokenEncrypted());
                graphClient.revokePermissions(token, config.getMetaUserId());
            } catch (Exception e) {
                log.warn("No s'han pogut revocar els permisos Meta per tenant {}: {}", tenantId, e.getMessage());
            }
        }

        config.setStatus(WhatsAppConnectionStatus.DISCONNECTED);
        config.setWebhookRegistered(false);
        config.setMetaUserId(null);
        configRepository.save(config);
        log.info("Meta WABA desconnectat per tenant {}", tenantId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String extractDisplayPhone(Map<String, Object> sessionData, String wabaId, String phoneNumberId) {
        try {
            var phones = (Map<String, List<Map<String, Object>>>) sessionData.get("phones");
            if (phones == null) return null;
            return phones.getOrDefault(wabaId, List.of()).stream()
                .filter(p -> phoneNumberId.equals(p.get("phoneNumberId")))
                .map(p -> (String) p.get("displayPhone"))
                .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String apiBaseUrl() {
        String url = sysConfig.get("API_BASE_URL");
        return (url != null && !url.isBlank()) ? url : "https://api.amgdl.com";
    }

    private String requireConfig(String key) {
        String v = sysConfig.get(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Configuració Meta no trobada: " + key);
        return v;
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
