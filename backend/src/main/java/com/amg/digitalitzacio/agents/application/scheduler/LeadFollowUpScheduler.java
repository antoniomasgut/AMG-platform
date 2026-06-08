package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeadFollowUpScheduler {

    private static final String REDIS_KEY      = "lead:followup:%s:%s";
    private static final int    REDIS_TTL_DAYS = 30;
    private static final int    DEFAULT_FOLLOWUP_DAYS = 3;
    private static final List<PipelineStage> STALE_STAGES = List.of(PipelineStage.NEW, PipelineStage.CONTACTED);

    private final TenantRepository         tenantRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final LeadRepository           leadRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final WhatsAppChannel          whatsAppChannel;
    private final WhatsAppMetaChannel      whatsAppMetaChannel;
    private final EmailChannel             emailChannel;
    private final StringRedisTemplate      redis;
    private final ObjectMapper             objectMapper;

    @Scheduled(cron = "0 30 8 * * *")
    public void sendLeadFollowUps() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F1");
        if (tenants.isEmpty()) return;

        log.info("[F1-LeadFollowUp] Comprovant leads paralitzats per {} tenants", tenants.size());

        for (var tenant : tenants) {
            try {
                processFollowUps(tenant.getId());
            } catch (Exception e) {
                log.warn("[F1-LeadFollowUp] Error processant seguiments per tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    private void processFollowUps(UUID tenantId) throws Exception {
        var agendaConfig = readConfig(tenantId, "AGENDA");
        int followupDays = agendaConfig.get("lead_followup_days") instanceof Number n ? n.intValue() : DEFAULT_FOLLOWUP_DAYS;
        if (followupDays <= 0) return;

        String template = agendaConfig.get("lead_followup_template") instanceof String t && !t.isBlank()
                ? t
                : "Hola {{nom}}! Et contactem perquè vas mostrar interès en els nostres serveis. Estem aquí si tens alguna pregunta o vols continuar. 😊";

        Instant cutoff = Instant.now().minus(followupDays, ChronoUnit.DAYS);
        var staleLeads = leadRepository.findStaleByTenantIdAndStages(tenantId, STALE_STAGES, cutoff);
        if (staleLeads.isEmpty()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        log.info("[F1-LeadFollowUp] {} leads paralitzats per tenant {} (>{} dies sense contacte)",
                staleLeads.size(), tenantId, followupDays);

        for (var lead : staleLeads) {
            String identifier = resolveIdentifier(lead);
            if (identifier == null) continue;

            String redisKey = REDIS_KEY.formatted(tenantId, lead.getId());
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            try {
                String name    = lead.getName() != null ? lead.getName().split(" ")[0] : "client";
                String message = template.replace("{{nom}}", name);
                sendFollowUp(chatLink, identifier, message);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                log.info("[F1-LeadFollowUp] Seguiment enviat al lead {} (tenant {})", lead.getId(), tenantId);
            } catch (Exception e) {
                log.warn("[F1-LeadFollowUp] Error enviant seguiment al lead {}: {}", lead.getId(), e.getMessage());
            }
        }
    }

    private String resolveIdentifier(Lead lead) {
        if (lead.getPhone() != null && !lead.getPhone().isBlank() && Boolean.TRUE.equals(lead.getHasWhatsapp())) {
            return lead.getPhone();
        }
        if (lead.getEmail() != null && !lead.getEmail().isBlank()) {
            return lead.getEmail();
        }
        return null;
    }

    private void sendFollowUp(TenantChatLink chatLink, String identifier, String message) {
        if (identifier.contains("@")) {
            emailChannel.sendMessage(identifier, "Seguiment de la teva consulta", message);
        } else if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), identifier, message);
        } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), identifier, message);
        } else {
            log.warn("[F1-LeadFollowUp] No hi ha canal configurat per enviar a {}", identifier);
        }
    }

    private Map<String, Object> readConfig(UUID tenantId, String key) {
        try {
            var opt = nexeConfigService.get(tenantId, key);
            if (opt.isEmpty()) return Map.of();
            return objectMapper.readValue(opt.get().getConfigJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
