package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.api.dto.PhaseHealthResponse;
import com.amg.digitalitzacio.auth.api.dto.TenantPhaseHealthResponse;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessConfigRepository;
import com.amg.digitalitzacio.payments.domain.StripeConfigRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// Servei de diagnòstic que comprova l'estat de configuració de cada fase contractada per un tenant
@Service
@RequiredArgsConstructor
public class PhaseHealthService {

    private final TenantRepository tenantRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final StripeConfigRepository stripeConfigRepository;
    private final GoCardlessConfigRepository goCardlessConfigRepository;
    private final SystemConfigService systemConfigService;
    private final PhasePrerequisiteValidator prerequisiteValidator;
    private final com.amg.digitalitzacio.agents.application.NexeServiceConfigService nexeConfigService;

    @Transactional(readOnly = true)
    public TenantPhaseHealthResponse checkHealth(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        // Parsejar les fases contractades (format CSV: "F1,F2,F3")
        String phases = tenant.getContractedPhases();
        List<String> contractedList = (phases != null && !phases.isBlank())
                ? Arrays.asList(phases.split(","))
                : List.of();
        var contractedSet = new HashSet<>(contractedList);

        // Comprovar l'estat de cada fase contractada
        var phaseHealthList = new ArrayList<PhaseHealthResponse>();
        for (String phase : contractedList) {
            phaseHealthList.add(checkPhase(tenantId, phase.trim()));
        }

        // Validar prerequisits entre fases
        var warnings = prerequisiteValidator.validate(contractedSet);
        String tenantName = tenant.getName() != null ? tenant.getName() : tenant.getEmail();

        return new TenantPhaseHealthResponse(tenantId, tenantName, contractedList, phaseHealthList, warnings);
    }

    // Delega el diagnòstic a cada mètode específic per fase
    private PhaseHealthResponse checkPhase(UUID tenantId, String phase) {
        return switch (phase) {
            case "F1" -> checkF1(tenantId);
            case "F2" -> checkF2(tenantId);
            case "F3" -> checkF3(tenantId);
            case "F4" -> checkF4(tenantId);
            case "F5" -> checkF5(tenantId);
            default   -> new PhaseHealthResponse(phase, true, List.of(), List.of());
        };
    }

    // F1 — Agent IA: necessita clau Anthropic, Telegram bot, i TenantChatLink actiu
    private PhaseHealthResponse checkF1(UUID tenantId) {
        var issues = new ArrayList<String>();
        var warnings = new ArrayList<String>();

        // Clau de l'agent IA (obligatòria)
        String anthropicKey = systemConfigService.get("ANTHROPIC_API_KEY");
        if (anthropicKey == null || anthropicKey.isBlank()) {
            issues.add("ANTHROPIC_API_KEY no configurada — l'agent IA no pot respondre");
        }

        // Token Telegram per a notificacions (obligatori)
        String telegramToken = systemConfigService.get("TELEGRAM_BOT_TOKEN");
        if (telegramToken == null || telegramToken.isBlank()) {
            issues.add("TELEGRAM_BOT_TOKEN no configurat — sense notificacions Telegram");
        }

        // Clau Brevo per a emails automàtics (avís no bloquejant)
        String brevoKey = systemConfigService.get("BREVO_API_KEY");
        if (brevoKey == null || brevoKey.isBlank()) {
            warnings.add("BREVO_API_KEY no configurada — no s'enviaran emails automàtics");
        }

        // TenantChatLink actiu — l'agent necessita un canal per rebre missatges
        boolean hasChatLink = chatLinkRepository.findByTenantId(tenantId)
                .map(cl -> Boolean.TRUE.equals(cl.getIsActive()))
                .orElse(false);
        if (!hasChatLink) {
            issues.add("Sense TenantChatLink actiu — l'agent no pot rebre missatges");
        }

        return new PhaseHealthResponse("F1", issues.isEmpty(), issues, warnings);
    }

    // F2 — Agenda: necessita el Service Account de Google Calendar
    private PhaseHealthResponse checkF2(UUID tenantId) {
        var issues = new ArrayList<String>();

        String calendarJson = systemConfigService.get("GOOGLE_CALENDAR_SA_JSON");
        if (calendarJson == null || calendarJson.isBlank()) {
            issues.add("GOOGLE_CALENDAR_SA_JSON no configurat — no es poden crear calendaris");
        }

        return new PhaseHealthResponse("F2", issues.isEmpty(), issues, List.of());
    }

    // F3 — Pressupostos/Pagaments: necessita almenys una passerela de pagament activa
    private PhaseHealthResponse checkF3(UUID tenantId) {
        var issues = new ArrayList<String>();
        var warnings = new ArrayList<String>();

        // Comprovar Stripe (actiu si té config i isActive=true)
        boolean hasStripe = stripeConfigRepository.findByTenantId(tenantId)
                .map(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElse(false);

        // Comprovar GoCardless (actiu si té apiKeyRef configurat)
        boolean hasGoCardless = goCardlessConfigRepository.findByTenantId(tenantId)
                .map(c -> c.getApiKeyRef() != null && !c.getApiKeyRef().isBlank())
                .orElse(false);

        if (!hasStripe && !hasGoCardless) {
            issues.add("Sense passerela de pagament — ni Stripe ni GoCardless configurat");
        } else if (!hasStripe) {
            warnings.add("Stripe no configurat — només accepta domiciliació SEPA (GoCardless)");
        }

        // Holded per a facturació (avís no bloquejant)
        String holdedKey = systemConfigService.get("HOLDED_API_KEY");
        if (holdedKey == null || holdedKey.isBlank()) {
            warnings.add("HOLDED_API_KEY no configurada — sense integració de facturació Holded");
        }

        return new PhaseHealthResponse("F3", issues.isEmpty(), issues, warnings);
    }

    // F4 — Fidelització: tots els automatismes llegeixen la config FIDELITZACIO;
    // sense config (o sense URL de ressenyes) la fase no fa res
    private PhaseHealthResponse checkF4(UUID tenantId) {
        var issues = new ArrayList<String>();
        var warnings = new ArrayList<String>();
        var config = nexeConfigService.get(tenantId, "FIDELITZACIO");
        if (config.isEmpty()) {
            issues.add("Config FIDELITZACIO no creada — cap automatisme de fidelització s'executarà");
        } else {
            String json = config.get().getConfigJson();
            if (json == null || !json.contains("google_reviews_url")
                    || json.contains("\"google_reviews_url\":\"\"")) {
                warnings.add("Sense URL de ressenyes de Google — no es demanaran ressenyes");
            }
        }
        return new PhaseHealthResponse("F4", issues.isEmpty(), issues, warnings);
    }

    // F5 — Equip: necessita la config EQUIP amb el grup de Telegram de l'equip
    private PhaseHealthResponse checkF5(UUID tenantId) {
        var issues = new ArrayList<String>();
        var warnings = new ArrayList<String>();
        var config = nexeConfigService.get(tenantId, "EQUIP");
        if (config.isEmpty()) {
            issues.add("Config EQUIP no creada — ni informes diaris ni alertes d'equip s'enviaran");
        } else {
            String json = config.get().getConfigJson();
            if (json == null || !json.contains("telegram_group_id")
                    || json.contains("\"telegram_group_id\":\"\"")) {
                issues.add("Sense grup de Telegram configurat — el bot no pot avisar l'equip");
            }
        }
        return new PhaseHealthResponse("F5", issues.isEmpty(), issues, warnings);
    }
}
