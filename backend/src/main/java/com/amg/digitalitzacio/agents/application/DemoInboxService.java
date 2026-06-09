package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.api.dto.ConversationResponse;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.demo.application.DemoLandingService;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoInboxService {

    private final DemoSessionRepository demoSessionRepository;
    private final ContactRepository contactRepository;
    private final ContactIdentifierRepository contactIdentifierRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationalAgentService conversationalAgentService;
    private final DemoLandingService demoLandingService;

    @Value("${app.demo.tenant-id:}")
    private String demoTenantId;

    private static final int DEMO_TTL_HOURS = 24;

    // Paraules explícitament bloquejades (CA/ES)
    private static final Set<String> BLOCKED_WORDS = Set.of(
        "puta", "puto", "putes", "hijo de puta", "joder", "coño", "cony",
        "cabron", "cabrón", "mierda", "merda", "idiota", "imbecil", "imbècil",
        "gilipollas", "gilipolla", "marica", "maricón", "polla", "cul",
        "foder", "fotut", "hostia", "ostia", "cago", "fuck", "shit", "bitch",
        "asshole", "bastard", "whore", "motherfucker"
    );

    @Transactional
    public DemoSession createSession(String prospectEmail, String companyName,
                                     String agentContext, String sector, String locale) {
        UUID token = UUID.randomUUID();
        String loc = locale != null && !locale.isBlank() ? locale : "ca";

        // Crea o reutilitza el tenant de demo per al sector i genera la landing
        var demoTenant = demoLandingService.getOrCreateDemoTenant(sector);
        String landingSlug = demoLandingService.createAndPublishDemoLanding(
                demoTenant.getId(), token, sector, companyName, loc);

        var session = DemoSession.builder()
                .token(token)
                .prospectEmail(prospectEmail != null ? prospectEmail.toLowerCase() : "demo@amgdl.com")
                .companyName(companyName)
                .agentContext(agentContext)
                .sector(sector)
                .locale(loc)
                .landingSlug(landingSlug)
                .isActive(true)
                .expiresAt(Instant.now().plus(DEMO_TTL_HOURS, ChronoUnit.HOURS))
                .build();
        return demoSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<DemoSession> listSessions() {
        return demoSessionRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Inicia una sessió de chat widget per a la demo.
     * No requereix siteId: usa DEMO_TENANT_ID directament.
     */
    public String startWidgetChat(UUID token) {
        var session = validateSession(token);
        UUID tenantId = getDemoTenantId();
        String sessionId = "demo:" + UUID.randomUUID();

        String greeting = conversationalAgentService.processWidgetMessage(
                tenantId, sessionId,
                "[SISTEMA: Visitant nou a la demo de " + (session.getCompanyName() != null
                        ? session.getCompanyName() : "l'empresa") + ". "
                + "Saluda'm breument i pregunta en què pots ajudar.]");

        if (greeting == null || greeting.isBlank()) {
            greeting = "Hola! Sóc el teu assistent virtual. En què et puc ajudar?";
        }
        return sessionId + "||" + greeting;
    }

    /**
     * Envia un missatge al widget de demo i retorna la resposta de l'agent.
     */
    public String sendWidgetMessage(UUID token, String sessionId, String text) {
        validateSession(token);

        String reason = checkModeration(text);
        if (reason != null) return "La conversa s'ha tancat per contingut inadequat.";

        UUID tenantId = getDemoTenantId();
        String reply = conversationalAgentService.processWidgetMessage(tenantId, sessionId, text);
        return reply != null ? reply : "Ho sent, no puc respondre ara. Torna-ho a provar.";
    }

    @Transactional
    public DemoSession updateSession(UUID token, String companyName, String agentContext) {
        var session = demoSessionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Demo no disponible"));
        if (companyName != null) session.setCompanyName(companyName);
        if (agentContext != null) session.setAgentContext(agentContext);
        return demoSessionRepository.save(session);
    }

    /** Valida token — llança 404 si no existeix, ha expirat, ha estat desactivada o bloquejada. */
    @Transactional(readOnly = true)
    public DemoSession validateSession(UUID token) {
        var session = demoSessionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Demo no disponible"));
        if (!Boolean.TRUE.equals(session.getIsActive())) {
            throw new ResourceNotFoundException("Demo no disponible");
        }
        if (Instant.now().isAfter(session.getExpiresAt())) {
            throw new ResourceNotFoundException("Demo no disponible");
        }
        return session;
    }

    @Transactional
    public void deactivateSession(UUID token) {
        var session = demoSessionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Demo no disponible"));
        session.setIsActive(false);
        demoSessionRepository.save(session);
        log.info("Demo session {} deactivated", token);
    }

    @Transactional(readOnly = true)
    public Optional<Contact> findContact(UUID tenantId, String prospectEmail) {
        return contactRepository.findByTenantIdAndEmail(tenantId, prospectEmail.toLowerCase());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getThread(UUID tenantId, String prospectEmail) {
        var contactOpt = findContact(tenantId, prospectEmail);
        if (contactOpt.isEmpty()) return List.of();

        var identifiers = contactIdentifierRepository.findByContactId(contactOpt.get().getId());
        var emailId = identifiers.stream()
                .filter(ci -> ci.getChannel() == ConversationChannel.EMAIL)
                .findFirst();
        if (emailId.isEmpty()) return List.of();

        return conversationRepository
                .findByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtAsc(
                        tenantId, emailId.get().getIdentifier(), ConversationChannel.EMAIL)
                .stream()
                .map(c -> new ConversationResponse(
                        c.getId(), c.getCustomerIdentifier(), c.getChannel(),
                        c.getRole(), c.getContent(), c.getPendingApproval(), c.getCreatedAt()))
                .toList();
    }

    /**
     * Envia un missatge del prospect. Primer comprova moderació; si falla, bloca la sessió.
     * Retorna el blockReason si s'ha bloquejat, null si tot bé.
     */
    @Transactional
    public String sendReply(DemoSession session, String text) {
        // Moderació per llista de paraules
        String reason = checkModeration(text);
        if (reason != null) {
            session.setBlockedAt(Instant.now());
            session.setBlockReason(reason);
            demoSessionRepository.save(session);
            log.warn("Demo session {} blocked: {}", session.getToken(), reason);
            return reason;
        }

        // Injecta el context de l'empresa al missatge (si n'hi ha)
        String enrichedText = enrichWithCompanyContext(session, text);

        var tenantId = getDemoTenantId();
        conversationalAgentService.handleIncoming(
                tenantId,
                session.getProspectEmail(),
                ConversationChannel.EMAIL,
                enrichedText);
        return null;
    }

    public UUID getDemoTenantId() {
        if (!StringUtils.hasText(demoTenantId)) {
            throw new IllegalStateException("DEMO_TENANT_ID not configured");
        }
        return UUID.fromString(demoTenantId);
    }

    // ── Privat ──────────────────────────────────────────────────────────────────

    private String checkModeration(String text) {
        if (text == null || text.isBlank()) return null;
        String lower = text.toLowerCase();
        for (String word : BLOCKED_WORDS) {
            // Comprova paraula completa o subseqüència de text
            if (lower.contains(word)) {
                return "Missatge amb contingut inadequat";
            }
        }
        return null;
    }

    /** Prepend company context as a system note only on the first message. */
    private String enrichWithCompanyContext(DemoSession session, String text) {
        boolean isFirst = getThread(getDemoTenantId(), session.getProspectEmail()).isEmpty();
        if (!isFirst) return text;

        var sb = new StringBuilder();
        sb.append("[Nota de context per a l'agent: ");
        if (StringUtils.hasText(session.getCompanyName())) {
            sb.append("Empresa prospect: ").append(session.getCompanyName()).append(". ");
        }
        if (StringUtils.hasText(session.getAgentContext())) {
            sb.append(session.getAgentContext());
        }
        sb.append("]\n\n").append(text);
        return sb.toString();
    }
}
