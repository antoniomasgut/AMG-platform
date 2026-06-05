package com.amg.digitalitzacio.leads.api;

import com.amg.digitalitzacio.leads.application.MetaLeadWebhookService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Webhook de Meta Lead Ads.
 *
 * Configuració a Facebook App:
 *   URL:          https://api.amgdl.com/api/v1/leads/meta-webhook
 *   Verify token: valor de META_WEBHOOK_VERIFY_TOKEN a SystemConfig
 *   Subscripcions: leadgen
 *
 * Per a cada tenant:
 *   - Configura metaPageId a TenantChatLink (camp "meta_page_id")
 *   - Afegeix META_PAGE_ACCESS_TOKEN_{PAGE_ID} a SystemConfig
 *   - O bé deixa META_PAGE_ACCESS_TOKEN per a una sola pàgina
 */
@RestController
@RequestMapping("/api/v1/leads/meta-webhook")
@RequiredArgsConstructor
@Slf4j
public class MetaLeadWebhookController {

    private final SystemConfigService sysConfig;
    private final MetaLeadWebhookService metaLeadWebhookService;

    /** Verificació del webhook per part de Meta (han de rebre el challenge de tornada). */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        String expected = sysConfig.get("META_WEBHOOK_VERIFY_TOKEN");
        if ("subscribe".equals(mode) && token.equals(expected)) {
            log.info("Meta webhook verificat correctament");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Meta webhook: token de verificació incorrecte");
        return ResponseEntity.status(403).body("Forbidden");
    }

    /** Recepció d'events de nous leads des de Meta. */
    @PostMapping
    public ResponseEntity<String> receive(@RequestBody Map<String, Object> payload) {
        try {
            processPayload(payload);
        } catch (Exception e) {
            log.error("Error processant webhook Meta leads: {}", e.getMessage());
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    @SuppressWarnings("unchecked")
    private void processPayload(Map<String, Object> payload) {
        if (!"page".equals(payload.get("object"))) return;

        var entries = (List<Map<String, Object>>) payload.getOrDefault("entry", List.of());
        for (var entry : entries) {
            var pageId = String.valueOf(entry.get("id"));
            var changes = (List<Map<String, Object>>) entry.getOrDefault("changes", List.of());
            for (var change : changes) {
                if (!"leadgen".equals(change.get("field"))) continue;
                var value = (Map<String, Object>) change.get("value");
                if (value == null) continue;

                var leadgenId = String.valueOf(value.get("leadgen_id"));
                var formId = String.valueOf(value.get("form_id"));
                metaLeadWebhookService.processLead(pageId, leadgenId, formId);
            }
        }
    }
}
