package com.amg.digitalitzacio.platform.application;

import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.engine.domain.LandingStatus;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessMandateRepository;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessMandateStatus;
import com.amg.digitalitzacio.platform.api.dto.PlatformRecommendation;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppConnectionStatus;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformRecommendationService {

    private final TenantRepository tenantRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantAIConfigRepository aiConfigRepository;
    private final WhatsAppWabaConfigRepository wabaConfigRepository;
    private final LandingRepository landingRepository;
    private final GoCardlessMandateRepository mandateRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Retorna recomanacions de categoria CLIENT_CONNECTIVITY:
     * coses que afecten directament la qualitat i connectivitat del servei del client.
     * Cap referència a proveïdors interns (Twilio, Meta, etc.).
     */
    @Transactional(readOnly = true)
    public List<PlatformRecommendation> forClient(UUID tenantId) {
        var recs = new ArrayList<PlatformRecommendation>();

        var chatLinkOpt = chatLinkRepository.findByTenantId(tenantId);
        long recentConversations = conversationRepository.countByTenantIdAndCreatedAtAfter(
                tenantId, Instant.now().minus(30, ChronoUnit.DAYS));

        // Canal de comunicació absent → el client perd totes les consultes
        boolean hasAnyChannel = chatLinkOpt.map(cl ->
                (cl.getWhatsappPhoneNumber() != null && !cl.getWhatsappPhoneNumber().isBlank()) ||
                (cl.getWhatsappMetaPhoneNumberId() != null && !cl.getWhatsappMetaPhoneNumberId().isBlank()) ||
                cl.getTelegramChatId() != null ||
                (cl.getEmailAddress() != null && !cl.getEmailAddress().isBlank())
        ).orElse(false);

        if (!hasAnyChannel) {
            recs.add(new PlatformRecommendation(
                    "CHANNEL",
                    "L'agent no pot rebre missatges",
                    "No tens cap canal de comunicació configurat. Els clients que intentin contactar no rebran resposta. Configura WhatsApp, Telegram o Email per activar l'agent.",
                    "HIGH",
                    "CLIENT_CONNECTIVITY"
            ));
        }

        // Agent en mode MANUAL amb activitat → perd temps de resposta
        boolean agentManual = chatLinkOpt.map(cl -> cl.getAgentMode() == AgentMode.MANUAL).orElse(false);
        if (agentManual && recentConversations > 0) {
            recs.add(new PlatformRecommendation(
                    "AGENT_MODE",
                    "L'agent no respon automàticament",
                    "El teu agent rep missatges però no respon sol. Amb mode Automàtic o Híbrid els clients reben resposta immediatament, sense esperar intervenció manual.",
                    "HIGH",
                    "CLIENT_CONNECTIVITY"
            ));
        }

        // KB massa buida amb activitat → respostes de baixa qualitat
        var kbOpt = knowledgeBaseRepository.findByTenantId(tenantId);
        int kbEntries = kbOpt.map(kb ->
                knowledgeEntryRepository.findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kb.getId()).size()
        ).orElse(0);

        if (recentConversations >= 5 && kbEntries < 3) {
            recs.add(new PlatformRecommendation(
                    "KNOWLEDGE_BASE",
                    "L'agent no té informació suficient",
                    "El teu agent rep consultes però té poca informació per respondre bé. Afegeix els teus serveis, horaris i preus perquè pugui donar respostes útils als clients.",
                    "MEDIUM",
                    "CLIENT_CONNECTIVITY"
            ));
        }

        // Web publicada sense domini → poca credibilitat
        var landings = landingRepository.findByTenantId(tenantId, Pageable.ofSize(10)).getContent();
        boolean hasPublished = landings.stream().anyMatch(l -> l.getStatus() == LandingStatus.PUBLISHED);
        boolean hasCustomDomain = landings.stream()
                .anyMatch(l -> l.getCustomDomain() != null && !l.getCustomDomain().isBlank());

        if (hasPublished && !hasCustomDomain) {
            recs.add(new PlatformRecommendation(
                    "LANDING",
                    "La teva web no té un domini propi",
                    "La teva pàgina web funciona però amb una adreça AMG. Un domini propi (exemple.com) genera més confiança als clients i millora la visibilitat als cercadors.",
                    "MEDIUM",
                    "CLIENT_CONNECTIVITY"
            ));
        }

        return recs;
    }

    /**
     * Retorna recomanacions per a l'administrador:
     * - UPGRADE_OFFER: millores que podem oferir al client (oportunitats de venda)
     * - COST_OPTIMIZATION: optimitzacions internes per reduir cost de tokens i millorar rendiment
     */
    @Transactional(readOnly = true)
    public List<PlatformRecommendation> forAdmin(UUID tenantId) {
        var recs = new ArrayList<PlatformRecommendation>();

        var tenantOpt = tenantRepository.findById(tenantId);
        var chatLinkOpt = chatLinkRepository.findByTenantId(tenantId);
        var aiConfigOpt = aiConfigRepository.findById(tenantId);
        long recentConversations = conversationRepository.countByTenantIdAndCreatedAtAfter(
                tenantId, Instant.now().minus(30, ChronoUnit.DAYS));

        Set<String> activePhases = new HashSet<>();
        if (tenantOpt.isPresent() && tenantOpt.get().getContractedPhases() != null) {
            Arrays.stream(tenantOpt.get().getContractedPhases().split(","))
                    .map(String::trim).filter(s -> !s.isBlank())
                    .forEach(activePhases::add);
        }

        // ── UPGRADE OFFERS ──────────────────────────────────────────────────

        // Twilio → Meta WABA: el client no nota la diferència però guanya en fiabilitat
        boolean hasTwilio = chatLinkOpt.map(cl ->
                cl.getWhatsappPhoneNumber() != null && !cl.getWhatsappPhoneNumber().isBlank()
        ).orElse(false);
        boolean hasMeta = wabaConfigRepository.findByTenantId(tenantId)
                .map(w -> w.getStatus() == WhatsAppConnectionStatus.CONNECTED)
                .orElse(false);

        if (hasTwilio && !hasMeta) {
            recs.add(new PlatformRecommendation(
                    "WHATSAPP_UPGRADE",
                    "Migrar a WhatsApp Business API (Meta)",
                    "Usa WhatsApp via número compartit Twilio. Amb l'API de Meta obtindria número exclusiu, plantilles aprovades i millor taxa de lliurament. Setup addicional: 50€.",
                    "MEDIUM",
                    "UPGRADE_OFFER"
            ));
        }

        // RAG upgrade: moltes converses però poca KB → la qualitat puja molt amb RAG
        var kbOpt = knowledgeBaseRepository.findByTenantId(tenantId);
        int kbEntries = kbOpt.map(kb ->
                knowledgeEntryRepository.findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kb.getId()).size()
        ).orElse(0);

        if (recentConversations >= 20 && kbEntries < 5) {
            recs.add(new PlatformRecommendation(
                    "RAG",
                    "Oferir mòdul RAG (base de coneixement vectorial)",
                    "Amb " + recentConversations + " converses/mes i poca KB, el mòdul RAG milloraria significativament la precisió. Oportunitat de venda: 100€ setup + 10€/mes.",
                    "HIGH",
                    "UPGRADE_OFFER"
            ));
        }

        // Expansió de fases
        if (activePhases.contains("F1") && recentConversations >= 10) {
            if (!activePhases.contains("F2")) {
                recs.add(new PlatformRecommendation(
                        "PHASE_F2",
                        "Oferir F2: Gestió de cites",
                        "F1 actiu amb activitat. Candidat a F2. Permet gestionar reserves automàticament. Ingressos addicionals: ~20€/mes (autònom) – 45€/mes (mitjana empresa).",
                        "HIGH",
                        "UPGRADE_OFFER"
                ));
            }
            if (activePhases.contains("F2") && !activePhases.contains("F3")) {
                recs.add(new PlatformRecommendation(
                        "PHASE_F3",
                        "Oferir F3: Pressupostos automàtics",
                        "Té F2 actiu. F3 automatitzaria el càlcul de pressupostos. Ingressos addicionals: ~20€/mes.",
                        "MEDIUM",
                        "UPGRADE_OFFER"
                ));
            }
        }

        // SEPA: client amb fases contractades però pagant manualment
        boolean hasPaidPhases = !activePhases.isEmpty();
        boolean hasActiveSepa = mandateRepository.findByTenantIdAndStatus(tenantId, GoCardlessMandateStatus.ACTIVE).isPresent();
        if (hasPaidPhases && !hasActiveSepa) {
            recs.add(new PlatformRecommendation(
                    "SEPA",
                    "Activar domiciliació SEPA (GoCardless)",
                    "Client de pagament sense SEPA. Redueix impagaments i elimina gestió manual de cobres. Comissió GoCardless: ~1% vs 2.9% Stripe.",
                    "MEDIUM",
                    "UPGRADE_OFFER"
            ));
        }

        // ── COST OPTIMIZATION ───────────────────────────────────────────────

        // Model car per a tenant de baix volum
        String model = aiConfigOpt.map(TenantAIConfig::getPreferredModel).orElse("claude-haiku-4-5-20251001");
        boolean isExpensiveModel = model.contains("sonnet") || model.contains("opus");
        if (isExpensiveModel && recentConversations > 0 && recentConversations < 30) {
            recs.add(new PlatformRecommendation(
                    "MODEL",
                    "Model IA sobredimensionat",
                    "Usa " + model + " amb " + recentConversations + " converses/mes. Amb Claude Haiku s'obtenen resultats equivalents a ~10× menys cost per a consultes estàndard.",
                    "HIGH",
                    "COST_OPTIMIZATION"
            ));
        }

        // Prompt massa llarg → molts tokens de context en cada crida
        String prompt = tenantOpt.map(t -> t.getAgentSystemPrompt()).orElse(null);
        if (prompt != null && prompt.length() > 2000) {
            recs.add(new PlatformRecommendation(
                    "PROMPT",
                    "Prompt del sistema molt llarg",
                    "El prompt té " + prompt.length() + " caràcters. Cada conversa envia tot el prompt com a context. Revisar i compactar podria reduir fins al 40% del cost de tokens.",
                    "MEDIUM",
                    "COST_OPTIMIZATION"
            ));
        }

        // Mode HYBRID amb molt baix volum → overhead innecessari
        boolean agentHybrid = chatLinkOpt.map(cl -> cl.getAgentMode() == AgentMode.HYBRID).orElse(false);
        if (agentHybrid && recentConversations < 5 && recentConversations > 0) {
            recs.add(new PlatformRecommendation(
                    "AGENT_MODE",
                    "Mode Híbrid amb poc volum",
                    "Amb " + recentConversations + " converses/mes en mode Híbrid, les aprovacions manuals generen overhead. AUTO simplificaria la gestió sense impacte perceptible.",
                    "LOW",
                    "COST_OPTIMIZATION"
            ));
        }

        // maxTokens molt alt → cada resposta pot costar molt
        int maxTokens = aiConfigOpt.map(TenantAIConfig::getMaxTokens).orElse(1024);
        if (maxTokens > 2048 && recentConversations > 0) {
            recs.add(new PlatformRecommendation(
                    "MAX_TOKENS",
                    "Límit de tokens per resposta elevat",
                    "maxTokens=" + maxTokens + ". Per a un agent de missatgeria, 512-1024 tokens per resposta és suficient. Reduir-lo disminuiria el cost per conversa.",
                    "MEDIUM",
                    "COST_OPTIMIZATION"
            ));
        }

        return recs;
    }
}
