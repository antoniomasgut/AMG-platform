package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.leads.domain.*;
import com.amg.digitalitzacio.shared.notification.NotificationEvent;
import com.amg.digitalitzacio.shared.notification.TenantNotificationService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Processa leads entrants de Meta Lead Ads.
 *
 * Flux:
 *  1. Webhook rep leadgen_id + page_id
 *  2. Es busca el tenant pel page_id (TenantChatLink.metaPageId)
 *  3. Es crida Graph API per obtenir les dades del lead
 *  4. Es crea el lead al CRM amb source=FACEBOOK o INSTAGRAM
 *
 * Config necessària a SystemConfig:
 *   META_PAGE_ACCESS_TOKEN_{PAGE_ID}  — token d'accés de la pàgina (per tenant)
 *   o META_PAGE_ACCESS_TOKEN           — token global si hi ha una sola pàgina
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetaLeadWebhookService {

    private static final String GRAPH_URL = "https://graph.facebook.com/v19.0/";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final TenantChatLinkRepository chatLinkRepository;
    private final LeadRepository leadRepository;
    private final SystemConfigService sysConfig;
    private final TenantNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void processLead(String pageId, String leadgenId, String formId) {
        // Busca tenant per page_id
        var chatLinkOpt = chatLinkRepository.findByMetaPageId(pageId);
        if (chatLinkOpt.isEmpty()) {
            log.warn("Meta webhook: cap tenant configurat per page_id={}", pageId);
            return;
        }
        var tenantId = chatLinkOpt.get().getTenantId();

        // Ja existeix?
        if (leadRepository.existsByTenantIdAndMetaLeadId(tenantId, leadgenId)) {
            log.debug("Meta lead {} ja processat, s'ignora", leadgenId);
            return;
        }

        // Token d'accés (per pàgina o global)
        var token = resolveToken(pageId);
        if (token == null) {
            log.error("Meta webhook: no hi ha PAGE_ACCESS_TOKEN per page_id={}", pageId);
            return;
        }

        try {
            // Crida a Graph API per obtenir dades del lead
            var url = GRAPH_URL + leadgenId + "?fields=field_data,created_time,platform&access_token=" + token;
            var req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            var res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != 200) {
                log.error("Meta Graph API retorna {} per leadgen_id={}", res.statusCode(), leadgenId);
                return;
            }

            var json = objectMapper.readTree(res.body());
            var lead = buildLead(tenantId, leadgenId, pageId, formId, json);
            leadRepository.save(lead);

            notificationService.notify(tenantId, NotificationEvent.LEAD_CREATED, Map.of(
                    "nom",     lead.getName() != null ? lead.getName() : "—",
                    "contact", lead.getEmail() != null ? lead.getEmail() : lead.getPhone() != null ? lead.getPhone() : "—",
                    "stage",   "Nou (Meta Lead Ads)"));

            log.info("Lead de Meta creat: tenant={} leadgenId={} name={}", tenantId, leadgenId, lead.getName());
        } catch (Exception e) {
            log.error("Error processant Meta lead {}: {}", leadgenId, e.getMessage());
        }
    }

    private Lead buildLead(java.util.UUID tenantId, String leadgenId, String pageId,
                            String formId, JsonNode json) {
        var lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setSource(detectSource(json));
        lead.setStage(PipelineStage.NEW);
        lead.setMetaLeadId(leadgenId);
        lead.setUtmSource(detectSource(json) == LeadSource.INSTAGRAM ? "instagram" : "facebook");
        lead.setUtmMedium("lead_ads");
        lead.setUtmCampaign("form_" + formId);

        var fieldData = json.path("field_data");
        for (var field : fieldData) {
            var name  = field.path("name").asText("");
            var value = field.path("values").path(0).asText("");
            switch (name) {
                case "full_name"     -> lead.setName(value);
                case "first_name"    -> { if (lead.getName() == null) lead.setName(value); }
                case "email"         -> lead.setEmail(value.toLowerCase());
                case "phone_number"  -> lead.setPhone(value);
            }
        }
        if (lead.getName() == null) lead.setName("Lead Meta Ads");

        return lead;
    }

    private LeadSource detectSource(JsonNode json) {
        var platform = json.path("platform").asText("");
        return "instagram".equalsIgnoreCase(platform) ? LeadSource.INSTAGRAM : LeadSource.FACEBOOK;
    }

    private String resolveToken(String pageId) {
        var specific = sysConfig.get("META_PAGE_ACCESS_TOKEN_" + pageId);
        if (specific != null && !specific.isBlank()) return specific;
        var global = sysConfig.get("META_PAGE_ACCESS_TOKEN");
        return (global != null && !global.isBlank()) ? global : null;
    }
}
