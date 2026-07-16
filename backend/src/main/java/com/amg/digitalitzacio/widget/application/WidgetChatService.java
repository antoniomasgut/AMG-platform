package com.amg.digitalitzacio.widget.application;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.agents.application.PromptBuilder;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.hosting.domain.WebSite;
import com.amg.digitalitzacio.hosting.domain.WebSiteRepository;
import com.amg.digitalitzacio.shared.notification.NotificationEvent;
import com.amg.digitalitzacio.shared.notification.TenantNotificationService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.widget.domain.WidgetSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetChatService {

    private static final String SESSION_KEY          = "wgt:session:";
    private static final String RATE_SESS_KEY        = "wgt:rate:sessions:";
    private static final String RATE_MSG_KEY         = "wgt:rate:messages:";
    private static final int    SESSION_TTL_H        = 2;
    private static final int    MAX_MSGS_PER_SESSION = 20;
    private static final int    MAX_SESSIONS_PER_IP  = 10;
    private static final int    MAX_MSGS_PER_IP_HOUR = 60;
    private static final String CHAT_MODEL           = "claude-haiku-4-5-20251001";
    private static final int    MAX_RESPONSE_TOKENS  = 600;
    private static final int    MAX_HISTORY_PAIRS    = 10;
    private static final int    MAX_INPUT_CHARS      = 500;
    private static final String ANTHROPIC_BASE       = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION    = "2023-06-01";

    private static final Set<String> PROFANITY = Set.of(
        "puta","puto","puteta","collons","cony","merda","gilipolles","cabron","cabrón",
        "hostia","ostia","hòstia","joder","coño","idiota","imbecil","imbècil","fotut",
        "fill de puta","hijo de puta","fuck","shit","asshole","bitch","bastard","cunt",
        "dick","pussy","motherfucker","subnormal","mongolo"
    );

    private final ConversationalAgentService conversationalAgentService;
    private final EmailService emailService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final WebSiteRepository webSiteRepository;
    private final TenantRepository tenantRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantAIConfigRepository aiConfigRepository;
    private final PromptBuilder promptBuilder;
    private final SystemConfigService sysConfig;
    private final TenantNotificationService notificationService;
    private final RestClient.Builder restClientBuilder;

    public record WidgetConfig(boolean chatEnabled, String waNumber, String businessName) {}
    public record CreateSessionResult(String sessionId, String greeting) {}
    public record SendMessageResult(String sessionId, String reply, boolean terminated) {}
    public record ContactFormResult(boolean ok, String redirectUrl) {}

    public WidgetConfig getConfig(UUID siteId) {
        var site = findActiveSite(siteId);
        var tenant = tenantRepository.findById(site.getTenantId()).orElse(null);
        var chatLink = chatLinkRepository.findByTenantId(site.getTenantId()).orElse(null);

        boolean agentRunning = chatLink != null && Boolean.TRUE.equals(chatLink.getIsActive());
        boolean chatEnabled = agentRunning && Boolean.TRUE.equals(chatLink.getWidgetEnabled())
                && aiConfigRepository.findByTenantId(site.getTenantId()).isPresent();

        String waNumber = resolveWaNumber(site.getTenantId(), tenant, chatLink);
        String businessName = tenant != null ? tenant.getName() : "";

        return new WidgetConfig(chatEnabled, waNumber, businessName);
    }

    public CreateSessionResult createSession(UUID siteId, String ip) {
        checkSessionRateLimit(ip);

        var site = findActiveSite(siteId);
        var tenant = tenantRepository.findById(site.getTenantId()).orElse(null);
        String businessName = tenant != null ? tenant.getName() : "Negoci";

        String systemPrompt = buildSystemPrompt(site.getTenantId());
        String greeting = generateGreeting(systemPrompt, businessName);

        var session = WidgetSession.builder()
                .id(UUID.randomUUID().toString())
                .siteId(siteId.toString())
                .tenantId(site.getTenantId().toString())
                .messageCount(0)
                .build();

        session.getMessages().add(new WidgetSession.Message("assistant", greeting));
        saveSession(session);
        incrementRateCounter(RATE_SESS_KEY + ip, 3600);

        notificationService.notify(site.getTenantId(), NotificationEvent.CHAT_WIDGET_NEW,
                Map.of("business_name", businessName));

        return new CreateSessionResult(session.getId(), greeting);
    }

    public SendMessageResult sendMessage(String sessionId, String userMessage, String ip) {
        checkMessageRateLimit(ip);

        if (userMessage == null || userMessage.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missatge buit");
        }
        if (userMessage.length() > MAX_INPUT_CHARS) {
            userMessage = userMessage.substring(0, MAX_INPUT_CHARS);
        }

        var session = loadSession(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessió no trobada o caducada");
        }

        if (session.getMessageCount() >= MAX_MSGS_PER_SESSION) {
            return new SendMessageResult(sessionId,
                "Conversa completada. Contacta'ns per telèfon o WhatsApp per a més informació.", true);
        }

        if (containsProfanity(userMessage)) {
            deleteSession(sessionId);
            return new SendMessageResult(sessionId,
                "La conversa s'ha tancat per incompliment de les normes d'ús.", true);
        }

        // Delega a l'agent conversacional si està actiu per al tenant (persista a PostgreSQL + inbox)
        UUID tenantId = UUID.fromString(session.getTenantId());
        String reply = tryAgentReply(tenantId, session.getId(), userMessage);

        if (reply == null) {
            // Fallback: camí Redis directe (agent no configurat)
            String systemPrompt = buildSystemPrompt(tenantId);
            var history = buildHistory(session);
            reply = callClaude(systemPrompt, history, userMessage);
            if (reply == null || reply.isBlank()) {
                reply = "Ho sent, en aquest moment no puc respondre. Torna-ho a provar en uns instants.";
            }
            session.getMessages().add(new WidgetSession.Message("user", userMessage));
            session.getMessages().add(new WidgetSession.Message("assistant", reply));
            trimHistory(session);
        }

        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastActivityAt(Instant.now());
        saveSession(session);
        incrementRateCounter(RATE_MSG_KEY + ip, 3600);

        return new SendMessageResult(sessionId, reply, false);
    }

    public ContactFormResult submitContactForm(UUID siteId, String name, String email,
                                               String phone, String message) {
        var site = findActiveSite(siteId);
        var tenant = tenantRepository.findById(site.getTenantId()).orElse(null);

        String toEmail = site.getContactEmail();
        if (toEmail == null || toEmail.isBlank()) {
            toEmail = tenant != null ? tenant.getEmail() : null;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[Widget] Contact form for site {} has no destination email configured", siteId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El formulari de contacte no està configurat");
        }

        String tenantName = tenant != null ? tenant.getName() : "Negoci";
        String subject = "Nou missatge del formulari web — " + tenantName;
        String body = "Has rebut un missatge des del formulari web de %s:\n\n".formatted(tenantName)
            + "Nom:     %s\n".formatted(name != null ? name : "—")
            + "Email:   %s\n".formatted(email != null ? email : "—")
            + "Telèfon: %s\n".formatted(phone != null && !phone.isBlank() ? phone : "—")
            + "\nMissatge:\n%s\n".formatted(message != null ? message : "—");

        emailService.sendEmail(toEmail, subject, body);
        log.info("[Widget] Contact form sent for site {} → {}", siteId, toEmail);

        return new ContactFormResult(true, site.getContactRedirectUrl());
    }

    // --- Helpers ---

    private String tryAgentReply(UUID tenantId, String sessionId, String text) {
        try {
            return conversationalAgentService.processWidgetMessage(tenantId, sessionId, text);
        } catch (Exception e) {
            log.warn("[Widget] Agent delegation failed, falling back to direct call: {}", e.getMessage());
            return null;
        }
    }

    private WebSite findActiveSite(UUID siteId) {
        return webSiteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));
    }

    private String resolveWaNumber(UUID tenantId, Tenant tenant, com.amg.digitalitzacio.agents.domain.TenantChatLink chatLink) {
        if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            return chatLink.getWhatsappPhoneNumber();
        }
        if (tenant != null && tenant.getContactPhone() != null && !tenant.getContactPhone().isBlank()) {
            return tenant.getContactPhone();
        }
        return null;
    }

    private String buildSystemPrompt(UUID tenantId) {
        try {
            return promptBuilder.build(tenantId, null);
        } catch (Exception e) {
            log.warn("Could not build prompt for tenant {}: {}", tenantId, e.getMessage());
            return "Ets l'assistent virtual d'aquest negoci. Respon de forma amigable i concisa en l'idioma del client.";
        }
    }

    private String generateGreeting(String systemPrompt, String businessName) {
        String fallback = "Hola! Sóc l'assistent virtual de " + businessName + ". En què puc ajudar-te?";
        try {
            String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return fallback;
            var result = callAnthropicApi(apiKey, systemPrompt +
                "\n\nGenera un missatge de benvinguda breu i amigable en català (màx. 2 frases). Presenta't pel nom del negoci i ofereix ajuda.",
                List.of(), "Hola");
            return (result != null && !result.isBlank()) ? result : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String callClaude(String systemPrompt, List<Map<String, String>> history, String userMessage) {
        try {
            String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return null;
            return callAnthropicApi(apiKey, systemPrompt, history, userMessage);
        } catch (Exception e) {
            log.error("Claude API error in widget chat: {}", e.getMessage());
            return null;
        }
    }

    private String callAnthropicApi(String apiKey, String systemPrompt,
                                     List<Map<String, String>> history, String userMessage) throws Exception {
        var messages = new ArrayList<>(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        var body = Map.of(
            "model", CHAT_MODEL,
            "max_tokens", MAX_RESPONSE_TOKENS,
            "system", systemPrompt,
            "messages", messages
        );
        var rc = restClientBuilder.baseUrl(ANTHROPIC_BASE).build();
        var raw = rc.post()
            .uri("/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .retrieve()
            .body(String.class);
        return objectMapper.readTree(raw).path("content").path(0).path("text").asText("");
    }

    private List<Map<String, String>> buildHistory(WidgetSession session) {
        var out  = new ArrayList<Map<String, String>>();
        var msgs = session.getMessages();
        int start = Math.max(0, msgs.size() - MAX_HISTORY_PAIRS * 2);
        for (int i = start; i < msgs.size(); i++) {
            var m = msgs.get(i);
            out.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        return out;
    }

    private void trimHistory(WidgetSession session) {
        int maxMsgs = MAX_HISTORY_PAIRS * 2 + 1;
        if (session.getMessages().size() > maxMsgs) {
            session.setMessages(new ArrayList<>(
                session.getMessages().subList(session.getMessages().size() - maxMsgs, session.getMessages().size())));
        }
    }

    private boolean containsProfanity(String text) {
        if (text == null) return false;
        var lower = text.toLowerCase();
        return PROFANITY.stream().anyMatch(lower::contains);
    }

    // --- Redis ---

    private void saveSession(WidgetSession session) {
        try {
            redis.opsForValue().set(SESSION_KEY + session.getId(),
                objectMapper.writeValueAsString(session), SESSION_TTL_H, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error saving widget session: {}", e.getMessage());
        }
    }

    private WidgetSession loadSession(String sessionId) {
        try {
            var raw = redis.opsForValue().get(SESSION_KEY + sessionId);
            if (raw == null) return null;
            return objectMapper.readValue(raw, WidgetSession.class);
        } catch (Exception e) {
            log.error("Error loading widget session: {}", e.getMessage());
            return null;
        }
    }

    private void deleteSession(String sessionId) {
        redis.delete(SESSION_KEY + sessionId);
    }

    private void incrementRateCounter(String key, int ttlSeconds) {
        var count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    private void checkSessionRateLimit(String ip) {
        var raw = redis.opsForValue().get(RATE_SESS_KEY + ip);
        if (raw != null && Long.parseLong(raw) >= MAX_SESSIONS_PER_IP) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Massa sessions. Torna-ho a provar en una hora.");
        }
    }

    private void checkMessageRateLimit(String ip) {
        var raw = redis.opsForValue().get(RATE_MSG_KEY + ip);
        if (raw != null && Long.parseLong(raw) >= MAX_MSGS_PER_IP_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Massa missatges. Torna-ho a provar en una hora.");
        }
    }
}
