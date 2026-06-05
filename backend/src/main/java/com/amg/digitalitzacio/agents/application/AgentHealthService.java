package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.api.dto.AgentHealthResponse;
import com.amg.digitalitzacio.agents.api.dto.AgentHealthResponse.HealthCheck;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentHealthService {

    private final TenantChatLinkRepository chatLinkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final ConversationRepository conversationRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public AgentHealthResponse calculate(UUID tenantId) {
        var checks = new ArrayList<HealthCheck>();
        var suggestions = new ArrayList<String>();

        var chatLinkOpt = chatLinkRepository.findByTenantId(tenantId);
        var tenantOpt = tenantRepository.findById(tenantId);

        // Active phases from tenant (respecta activePhases si s'ha configurat)
        Set<String> activePhases = new HashSet<>();
        tenantOpt.ifPresent(t -> {
            var source = t.getActivePhases() != null ? t.getActivePhases() : t.getContractedPhases();
            if (source != null) Arrays.stream(source.split(","))
                    .map(String::trim).filter(s -> !s.isBlank())
                    .forEach(activePhases::add);
        });

        // Knowledge entries by category
        Set<KnowledgeCategory> filledCategories = new HashSet<>();
        var kbOpt = knowledgeBaseRepository.findByTenantId(tenantId);
        if (kbOpt.isPresent()) {
            knowledgeEntryRepository
                    .findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kbOpt.get().getId())
                    .forEach(e -> filledCategories.add(e.getCategory()));
        }

        // 1. Canals configurats (25 pts)
        boolean hasChannel = chatLinkOpt.map(cl ->
                (cl.getWhatsappPhoneNumber() != null && !cl.getWhatsappPhoneNumber().isBlank()) ||
                (cl.getTelegramChatId() != null) ||
                (cl.getEmailAddress() != null && !cl.getEmailAddress().isBlank())
        ).orElse(false);

        if (hasChannel) {
            var configured = channelList(chatLinkOpt.get());
            checks.add(new HealthCheck("CHANNEL", "Canals de comunicació", "OK",
                    "Configurat: " + configured));
        } else {
            checks.add(new HealthCheck("CHANNEL", "Canals de comunicació", "ERROR",
                    "Cap canal configurat (WhatsApp, Telegram o Email)"));
            suggestions.add("Configura almenys un canal de comunicació (WhatsApp recomanat) per activar l'agent");
        }

        // 2. Agent actiu (15 pts)
        boolean agentActive = chatLinkOpt.map(cl ->
                cl.getAgentMode() == AgentMode.AUTO || cl.getAgentMode() == AgentMode.HYBRID
        ).orElse(false);

        if (agentActive) {
            var mode = chatLinkOpt.get().getAgentMode().name();
            checks.add(new HealthCheck("AGENT_MODE", "Mode de l'agent", "OK",
                    "Mode " + mode + ": l'agent respon " + (mode.equals("AUTO") ? "automàticament" : "amb revisió prèvia")));
        } else {
            var mode = chatLinkOpt.map(cl -> cl.getAgentMode().name()).orElse("MANUAL");
            checks.add(new HealthCheck("AGENT_MODE", "Mode de l'agent", "WARNING",
                    "Mode " + mode + ": l'agent no respon automàticament"));
            suggestions.add("Activa el mode AUTO o HYBRID a la configuració de l'agent per respondre missatges");
        }

        // 3. Informació bàsica (F1) (20 pts)
        boolean hasBusinessInfo = filledCategories.contains(KnowledgeCategory.BUSINESS_INFO);
        boolean hasSchedule = filledCategories.contains(KnowledgeCategory.SCHEDULE);
        boolean hasService = filledCategories.contains(KnowledgeCategory.SERVICE);

        int f1Points = (hasBusinessInfo ? 1 : 0) + (hasSchedule ? 1 : 0) + (hasService ? 1 : 0);
        if (f1Points == 3) {
            checks.add(new HealthCheck("KNOWLEDGE_BASE", "Informació bàsica (F1)", "OK",
                    "Informació del negoci, horaris i serveis configurats"));
        } else {
            var missing = new ArrayList<String>();
            if (!hasBusinessInfo) missing.add("informació del negoci");
            if (!hasSchedule) missing.add("horaris");
            if (!hasService) missing.add("serveis i preus");
            checks.add(new HealthCheck("KNOWLEDGE_BASE", "Informació bàsica (F1)",
                    f1Points == 0 ? "ERROR" : "WARNING",
                    "Falta: " + String.join(", ", missing)));
            suggestions.add("Omple la base de coneixement: " + String.join(", ", missing));
        }

        // 4. Cobertura per fases actives (25 pts)
        var phaseMissing = new ArrayList<String>();
        if (activePhases.contains("F2") && !filledCategories.contains(KnowledgeCategory.BOOKING_RULES)) {
            phaseMissing.add("Agenda");
        }
        if (activePhases.contains("F3") && !filledCategories.contains(KnowledgeCategory.QUOTE_RULES)) {
            phaseMissing.add("Pressupostos");
        }
        if (activePhases.contains("F4") && !filledCategories.contains(KnowledgeCategory.FOLLOWUP_RULES)) {
            phaseMissing.add("Seguiment");
        }
        if (activePhases.contains("F5") && !filledCategories.contains(KnowledgeCategory.TEAM_INFO)) {
            phaseMissing.add("Alertes & Equip");
        }

        var phaseNames = new java.util.LinkedHashMap<String, String>();
        phaseNames.put("F1", "Captació");
        phaseNames.put("F2", "Agenda");
        phaseNames.put("F3", "Pressupostos");
        phaseNames.put("F4", "Seguiment");
        phaseNames.put("F5", "Alertes & Equip");
        var activePhaseNames = activePhases.stream()
                .map(f -> phaseNames.getOrDefault(f, f))
                .collect(java.util.stream.Collectors.joining(", "));

        if (!activePhases.isEmpty()) {
            if (phaseMissing.isEmpty()) {
                checks.add(new HealthCheck("PHASE_COVERAGE", "Cobertura per fases", "OK",
                        "Totes les fases actives (" + activePhaseNames + ") estan configurades"));
            } else {
                checks.add(new HealthCheck("PHASE_COVERAGE", "Cobertura per fases", "WARNING",
                        "Fases sense configuració: " + String.join(", ", phaseMissing)));
                phaseMissing.forEach(m -> suggestions.add("Completa la configuració de: " + m));
            }
        }

        // 5. Activitat recent (15 pts)
        long recentConversations = conversationRepository.countByTenantIdAndCreatedAtAfter(
                tenantId, Instant.now().minus(30, ChronoUnit.DAYS));

        if (recentConversations > 0) {
            checks.add(new HealthCheck("ACTIVITY", "Activitat recent", "OK",
                    recentConversations + " converses en els darrers 30 dies"));
        } else {
            checks.add(new HealthCheck("ACTIVITY", "Activitat recent", "WARNING",
                    "Cap conversa en els darrers 30 dies"));
            if (hasChannel) {
                suggestions.add("L'agent té canals configurats però no ha rebut missatges. Comprova que el webhook és actiu.");
            }
        }

        // Score
        int score = 0;
        if (hasChannel) score += 25;
        if (agentActive) score += 15;
        score += f1Points * 7; // 0, 7, 14, 21
        if (phaseMissing.isEmpty() && !activePhases.isEmpty()) score += 25;
        else if (!activePhases.isEmpty()) score += Math.max(0, 25 - phaseMissing.size() * 8);
        else score += 25; // no phases contracted → not penalised
        if (recentConversations > 0) score += 14;
        score = Math.min(100, score);

        // Recomanacions de millora de rendiment (no són errors, són oportunitats)
        var recommendations = new ArrayList<String>();

        // RAG: si hi ha activitat notable i la base de coneixement és bàsica
        if (recentConversations >= 30) {
            recommendations.add("Amb " + recentConversations + " converses mensuals, el mòdul RAG convertiria documents i PDFs en coneixement vectorial per a respostes molt més precises");
        } else if (recentConversations >= 15) {
            recommendations.add("La base de coneixement vectorial (RAG) milloraria la precisió de les respostes a mesura que l'activitat creix");
        }

        // WhatsApp: si no té WhatsApp però té Telegram o email
        boolean hasWhatsApp = chatLinkOpt.map(cl ->
                (cl.getWhatsappPhoneNumber() != null && !cl.getWhatsappPhoneNumber().isBlank()) ||
                (cl.getWhatsappMetaPhoneNumberId() != null && !cl.getWhatsappMetaPhoneNumberId().isBlank())
        ).orElse(false);
        boolean hasTelegramOrEmail = chatLinkOpt.map(cl ->
                cl.getTelegramChatId() != null ||
                (cl.getEmailAddress() != null && !cl.getEmailAddress().isBlank())
        ).orElse(false);
        if (!hasWhatsApp && hasTelegramOrEmail) {
            recommendations.add("Afegir WhatsApp Business ampliar la cobertura: és el canal de missatgeria preferit i genera un 3× més d'interaccions que Telegram");
        }

        // Fases addicionals: si F1 està ben configurat i hi ha activitat
        boolean f1Complete = f1Points == 3 && agentActive && recentConversations > 0;
        if (f1Complete) {
            if (!activePhases.contains("F2")) {
                recommendations.add("La fase Agenda (F2) permetria a l'agent gestionar reserves automàticament, estalviant temps al teu equip");
            }
            if (activePhases.contains("F2") && !activePhases.contains("F3")) {
                recommendations.add("La fase Pressupostos (F3) automatitzaria el càlcul d'ofertes, convertint més consultes en clients");
            }
            if (activePhases.contains("F3") && !activePhases.contains("F4")) {
                recommendations.add("La fase Seguiment (F4) faria seguiment automàtic de clients existents per generar repetició de negoci");
            }
        }

        // Mode AUTO: si és HYBRID o MANUAL amb activitat alta
        boolean isManualOrHybrid = chatLinkOpt.map(cl -> cl.getAgentMode() != AgentMode.AUTO).orElse(false);
        if (isManualOrHybrid && recentConversations >= 10) {
            recommendations.add("Amb " + recentConversations + " converses mensuals, el mode AUTO respondria immediatament sense aprovació manual i milloraria la satisfacció del client");
        }

        String status = score >= 80 ? "GREEN" : score >= 50 ? "YELLOW" : "RED";
        return new AgentHealthResponse(score, status, checks, suggestions, recommendations);
    }


    private String channelList(TenantChatLink cl) {
        var list = new ArrayList<String>();
        if (cl.getWhatsappPhoneNumber() != null && !cl.getWhatsappPhoneNumber().isBlank()) list.add("WhatsApp");
        if (cl.getTelegramChatId() != null) list.add("Telegram");
        if (cl.getEmailAddress() != null && !cl.getEmailAddress().isBlank()) list.add("Email");
        return String.join(", ", list);
    }
}
