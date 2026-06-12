package com.amg.digitalitzacio.chat.application;

import com.amg.digitalitzacio.agents.application.ChannelUsageService;
import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.PromptBuilder;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.chat.domain.ChatSession;
import com.amg.digitalitzacio.chat.domain.LandingChatContext;
import com.amg.digitalitzacio.chat.domain.LandingChatContextRepository;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final String SESSION_KEY          = "chat:session:";
    private static final String RATE_SESS_KEY        = "chat:rate:sessions:";
    private static final String RATE_MSG_KEY         = "chat:rate:messages:";
    private static final int    SESSION_TTL_H        = 2;
    private static final int    MAX_MSGS_PER_SESSION = 20;
    private static final int    MAX_SESSIONS_PER_IP  = 10;
    private static final int    MAX_MSGS_PER_IP_HOUR = 60;
    private static final String CHAT_MODEL_DEFAULT    = "claude-haiku-4-5-20251001";
    private static final String DEEPSEEK_BASE         = "https://api.deepseek.com";
    private static final int    MAX_RESPONSE_TOKENS  = 300;
    private static final int    MAX_HISTORY_PAIRS    = 10;
    private static final int    MAX_INPUT_CHARS      = 500;
    private static final String ANTHROPIC_BASE       = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION    = "2023-06-01";
    private static final Pattern BOOKING_TAG         = Pattern.compile("\\[CONFIRMA_CITA:(\\{.*?\\})]", Pattern.DOTALL);

    // Paraules malsonants — CA/ES/EN més comunes
    private static final Set<String> PROFANITY = Set.of(
        "puta","puto","puteta","collons","cony","merda","gilipolles","cabron","cabrón",
        "hostia","ostia","hòstia","joder","coño","idiota","imbecil","imbècil","fotut",
        "fill de puta","hijo de puta","fuck","shit","asshole","bitch","bastard","cunt",
        "dick","pussy","motherfucker","subnormal","mongolo"
    );

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final LandingChatContextRepository chatContextRepository;
    private final LandingRepository landingRepository;
    private final LeadRepository leadRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final PromptBuilder promptBuilder;
    private final GoogleCalendarService googleCalendarService;
    private final SystemConfigService sysConfig;
    private final RestClient.Builder restClientBuilder;
    private final ChannelUsageService channelUsageService;
    private final TenantRepository tenantRepository;
    private final TenantAIConfigRepository aiConfigRepository;
    private final TenantChatLinkRepository chatLinkRepository;

    @Value("${app.landing.base-domain:webs.amgdl.com}")
    private String landingBaseDomain;

    public record CreateSessionResult(String sessionId, String greeting) {}
    public record SendMessageResult(String sessionId, String reply, boolean terminated) {}

    public CreateSessionResult createSession(String landingSlug, String contactName, String contactPhone, String ip) {
        checkSessionRateLimit(ip);

        var landing = landingRepository.findBySlug(landingSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Landing not found"));

        var ctx = chatContextRepository.findById(landing.getId())
                .orElseGet(() -> buildGenericContext(landing.getId(), landing.getTitle()));

        var tenantId  = landing.getTenantId();
        var sessionId = UUID.randomUUID().toString();

        // Comprova si l'agenda està activada per a aquest tenant
        boolean agendaEnabled = isAgendaEnabledForTenant(tenantId);
        String systemPrompt   = ctx.getSystemPrompt();
        if (agendaEnabled) {
            var agendaJson = nexeConfigService.get(tenantId, "AGENDA")
                    .map(c -> c.getConfigJson()).orElse(null);
            if (agendaJson != null) systemPrompt += promptBuilder.buildAgendaBlock(agendaJson);
        }

        var greeting = generateGreetingWithPrompt(ctx, systemPrompt, landingSlug);

        var session = ChatSession.builder()
                .id(sessionId)
                .landingSlug(landingSlug)
                .landingId(landing.getId().toString())
                .tenantId(tenantId.toString())
                .contactName(contactName)
                .contactPhone(contactPhone)
                .agendaEnabled(agendaEnabled)
                .messageCount(0)
                .build();

        // Crea o reutilitza Lead si s'ha proporcionat informació de contacte
        if (contactName != null && !contactName.isBlank() &&
            contactPhone != null && !contactPhone.isBlank()) {
            try {
                var lead = findOrCreateChatLead(landing.getTenantId(), contactName.strip(), contactPhone.strip());
                session.setLeadId(lead.getId().toString());
            } catch (Exception e) {
                log.warn("Could not create lead for chat session: {}", e.getMessage());
            }
        }

        session.getMessages().add(new ChatSession.ChatMessage("assistant", greeting));
        saveSession(session);
        incrementRateCounter(RATE_SESS_KEY + ip, 3600);

        return new CreateSessionResult(sessionId, greeting);
    }

    public boolean isAgencyChatEnabled() {
        return tenantRepository.findByIsOwnerTrue()
                .map(owner -> chatLinkRepository.findByTenantId(owner.getId())
                        .map(link -> Boolean.TRUE.equals(link.getIsActive())
                                && Boolean.TRUE.equals(link.getWidgetEnabled()))
                        .orElse(false))
                .orElse(false);
    }

    public CreateSessionResult createAgencySession(String contactName, String contactPhone, String ip) {
        checkSessionRateLimit(ip);

        var owner = tenantRepository.findByIsOwnerTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner tenant not configured"));
        var tenantId    = owner.getId();
        var tenantIdStr = tenantId.toString();

        String systemPrompt = promptBuilder.build(tenantId, null);
        var ctx = chatContextRepository.findById(tenantId)
                .orElseGet(() -> buildDefaultAgencyContext(tenantId));
        ctx = LandingChatContext.builder()
                .landingId(ctx.getLandingId())
                .businessName(ctx.getBusinessName())
                .systemPrompt(systemPrompt)
                .build();

        var aiCfg = aiConfigRepository.findByTenantId(tenantId).orElse(TenantAIConfig.defaultFor(tenantId));
        var model = (aiCfg.getPreferredModel() != null && !aiCfg.getPreferredModel().isBlank())
                ? aiCfg.getPreferredModel() : CHAT_MODEL_DEFAULT;

        var sessionId = UUID.randomUUID().toString();
        var greeting  = generateGreetingWithPrompt(ctx, systemPrompt, "agency", model);

        var session = ChatSession.builder()
                .id(sessionId)
                .landingSlug("agency")
                .landingId(tenantIdStr)
                .tenantId(tenantIdStr)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .preferredModel(model)
                .agendaEnabled(false)
                .messageCount(0)
                .build();

        session.getMessages().add(new ChatSession.ChatMessage("assistant", greeting));
        saveSession(session);
        incrementRateCounter(RATE_SESS_KEY + ip, 3600);

        // Registra lead si tenim nom + telèfon
        if (contactName != null && !contactName.isBlank()
                && contactPhone != null && !contactPhone.isBlank()) {
            try {
                findOrCreateChatLead(tenantId, contactName.strip(), contactPhone.strip());
            } catch (Exception e) {
                log.warn("[Agency] Could not create lead for {}: {}", contactName, e.getMessage());
            }
        }

        return new CreateSessionResult(sessionId, greeting);
    }

    private LandingChatContext buildDefaultAgencyContext(UUID tenantId) {
        return LandingChatContext.builder()
                .landingId(tenantId)
                .businessName("AMG Digitalitzacions")
                .systemPrompt("Ets l'assistent virtual d'AMG Digitalitzacions, una agència digital de Mallorca especialitzada en digitalització de negocis locals. Ajudes els visitants a entendre els serveis (landings, WhatsApp Business, agents IA, automatitzacions) i a demanar informació o pressupost. Respon en l'idioma del visitant (català, castellà, anglès o alemany). Sigues amable, concís i professional.")
                .build();
    }

    private boolean isAgendaEnabledForTenant(UUID tenantId) {
        return nexeConfigService.get(tenantId, "AGENDA").map(cfg -> {
            try {
                Map<String, Object> c = objectMapper.readValue(cfg.getConfigJson(), new TypeReference<>() {});
                Object enabled = c.get("enabled");
                return Boolean.TRUE.equals(enabled);
            } catch (Exception e) { return false; }
        }).orElse(false);
    }

    private String generateGreetingWithPrompt(LandingChatContext ctx, String systemPrompt, String landingSlug) {
        return generateGreetingWithPrompt(ctx, systemPrompt, landingSlug, null);
    }

    private String generateGreetingWithPrompt(LandingChatContext ctx, String systemPrompt, String landingSlug, String modelOverride) {
        String fallback = "Hola! Sóc l'assistent virtual de " + ctx.getBusinessName() + ". En què puc ajudar-te?";
        try {
            String greetingPrompt = systemPrompt +
                "\n\nGenera un missatge de benvinguda breu i amigable en català (màx. 2 frases). Presenta't pel nom del negoci i ofereix ajuda.";
            var result = callAI(greetingPrompt, List.of(), "Hola", isDemo(landingSlug), modelOverride);
            return (result != null && !result.isBlank()) ? result : fallback;
        } catch (Exception e) {
            log.warn("Could not generate greeting: {}", e.getMessage());
            return fallback;
        }
    }

    private String processBookingTag(String response, UUID tenantId, String leadId) {
        Matcher m = BOOKING_TAG.matcher(response);
        if (!m.find()) return response;
        String cleaned = BOOKING_TAG.matcher(response).replaceAll("").strip();
        try {
            Map<String, Object> booking = objectMapper.readValue(m.group(1), new TypeReference<>() {});
            var agendaCfg = nexeConfigService.get(tenantId, "AGENDA").orElse(null);
            if (agendaCfg == null) return cleaned;
            Map<String, Object> agenda = objectMapper.readValue(agendaCfg.getConfigJson(), new TypeReference<>() {});
            String calType = String.valueOf(agenda.getOrDefault("calendar_type", ""));
            if (!"google".equals(calType) && !"google_oauth".equals(calType)) return cleaned;
            String calId = (String) agenda.get("google_calendar_id");
            if (calId == null || calId.isBlank()) return cleaned;
            String date = (String) booking.get("date");
            String time = (String) booking.get("time");
            if (date == null || time == null) return cleaned;
            LocalDateTime start = LocalDateTime.parse(date + "T" + time,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            int duration = booking.get("duration") instanceof Number n ? n.intValue() : 60;
            String name  = booking.get("name") instanceof String s ? s : "Client";
            String notes = booking.get("notes") instanceof String s ? s : "";
            if ("google_oauth".equals(calType)) {
                googleCalendarService.createEventOAuth((String) agenda.get("google_refresh_token"), calId,
                        "Visita: " + name, start, duration, notes);
            } else {
                googleCalendarService.createEvent(calId, "Visita: " + name, start, duration, notes);
            }
            // Actualitza lastServiceAt del lead quan es confirma una cita
            if (leadId != null) {
                try {
                    leadRepository.findById(UUID.fromString(leadId)).ifPresent(lead -> {
                        lead.setLastServiceAt(Instant.now());
                        leadRepository.save(lead);
                    });
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Could not process booking tag: {}", e.getMessage());
        }
        return cleaned;
    }

    private Lead findOrCreateChatLead(UUID tenantId, String name, String phone) {
        return leadRepository.findFirstByTenantIdAndPhone(tenantId, phone)
                .map(lead -> {
                    lead.setLastContactAt(Instant.now());
                    return leadRepository.save(lead);
                })
                .orElseGet(() -> {
                    var lead = new Lead();
                    lead.setTenantId(tenantId);
                    lead.setName(name);
                    lead.setPhone(phone);
                    lead.setSource(LeadSource.CHAT_WIDGET);
                    lead.setLastContactAt(Instant.now());
                    return leadRepository.save(lead);
                });
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
                "Conversa completada. Contacta'ns per telèfon per a més informació.", true);
        }

        if (containsProfanity(userMessage)) {
            log.info("Profanity detected, closing session {}", sessionId);
            deleteSession(sessionId);
            return new SendMessageResult(sessionId,
                "La conversa s'ha tancat per incompliment de les normes d'ús.", true);
        }

        var ctx = loadContext(session.getLandingId());
        String basePrompt   = ctx != null ? ctx.getSystemPrompt() : buildGenericSystemPrompt(session.getLandingSlug());
        String systemPrompt = basePrompt;
        if (session.isAgendaEnabled() && session.getTenantId() != null) {
            var agendaJson = nexeConfigService.get(UUID.fromString(session.getTenantId()), "AGENDA")
                    .map(c -> c.getConfigJson()).orElse(null);
            if (agendaJson != null) systemPrompt += promptBuilder.buildAgendaBlock(agendaJson);
        }

        var history = buildHistory(session);
        String reply = callAI(systemPrompt, history, userMessage, isDemo(session.getLandingSlug()), session.getPreferredModel());
        if (reply == null || reply.isBlank()) {
            reply = "Ho sent, en aquest moment no puc respondre. Torna-ho a provar en uns instants.";
        }

        // Processa tag de reserva si l'agenda és activa
        if (session.isAgendaEnabled() && session.getTenantId() != null) {
            try {
                reply = processBookingTag(reply, UUID.fromString(session.getTenantId()), session.getLeadId());
            } catch (Exception e) {
                log.warn("Error processing booking tag in chat: {}", e.getMessage());
            }
        }

        // Actualitza lastContactAt del lead (best-effort)
        if (session.getLeadId() != null) {
            try {
                leadRepository.findById(UUID.fromString(session.getLeadId())).ifPresent(lead -> {
                    lead.setLastContactAt(Instant.now());
                    leadRepository.save(lead);
                });
            } catch (Exception ignored) {}
        }

        session.getMessages().add(new ChatSession.ChatMessage("user", userMessage));
        session.getMessages().add(new ChatSession.ChatMessage("assistant", reply));
        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastActivityAt(Instant.now());
        trimHistory(session);
        saveSession(session);
        incrementRateCounter(RATE_MSG_KEY + ip, 3600);

        // Registra l'ús del canal de xat (best-effort; agency sessions no tenen landing real)
        try {
            if (!"agency".equals(session.getLandingSlug())) {
                landingRepository.findById(UUID.fromString(session.getLandingId()))
                        .ifPresent(l -> channelUsageService.record(l.getTenantId(), ChannelUsageService.CHAT));
            }
        } catch (Exception ignored) {}

        return new SendMessageResult(sessionId, reply, false);
    }

    // --- AI dispatch ---

    private boolean isDemo(String landingSlug) {
        return landingSlug != null && landingSlug.startsWith("demo-");
    }

    /**
     * Dispatches the AI call to the configured provider.
     * Demo sessions use DEMO_AI_PROVIDER + DEMO_AI_MODEL from SystemConfig;
     * non-demo sessions use Anthropic with modelOverride (or CHAT_MODEL_DEFAULT if null).
     */
    private String callAI(String systemPrompt, List<Map<String, String>> history,
                          String userMessage, boolean demo) {
        return callAI(systemPrompt, history, userMessage, demo, null);
    }

    private String callAI(String systemPrompt, List<Map<String, String>> history,
                          String userMessage, boolean demo, String modelOverride) {
        try {
            if (demo) {
                String provider = sysConfig.get("DEMO_AI_PROVIDER");
                String model    = sysConfig.get("DEMO_AI_MODEL");
                if (provider == null || provider.isBlank()) provider = "anthropic";
                if (model    == null || model.isBlank())    model    = CHAT_MODEL_DEFAULT;

                if ("deepseek".equalsIgnoreCase(provider)) {
                    String apiKey = sysConfig.get("DEEPSEEK_API_KEY");
                    if (apiKey == null || apiKey.isBlank()) {
                        log.warn("DEEPSEEK_API_KEY not configured, falling back to Anthropic for demo chat");
                    } else {
                        return callOpenAICompatibleApi(apiKey, DEEPSEEK_BASE, model,
                                systemPrompt, history, userMessage);
                    }
                }
                // anthropic (or deepseek fallback)
                String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isBlank()) return null;
                return callAnthropicApi(apiKey, model, systemPrompt, history, userMessage);
            } else {
                String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isBlank()) return null;
                String model = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : CHAT_MODEL_DEFAULT;
                return callAnthropicApi(apiKey, model, systemPrompt, history, userMessage);
            }
        } catch (Exception e) {
            log.error("AI API error in chat (demo={}): {}", demo, e.getMessage());
            return null;
        }
    }

    private String callAnthropicApi(String apiKey, String model, String systemPrompt,
                                     List<Map<String, String>> history, String userMessage) throws Exception {
        var messages = new ArrayList<>(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        var body = Map.of(
            "model",      model,
            "max_tokens", MAX_RESPONSE_TOKENS,
            "system",     systemPrompt,
            "messages",   messages
        );
        var rc  = restClientBuilder.baseUrl(ANTHROPIC_BASE).build();
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

    /** OpenAI-compatible API (DeepSeek, OpenAI, etc.) */
    private String callOpenAICompatibleApi(String apiKey, String baseUrl, String model,
                                            String systemPrompt, List<Map<String, String>> history,
                                            String userMessage) throws Exception {
        var messages = new ArrayList<Map<String, String>>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        var body = Map.of(
            "model",      model,
            "max_tokens", MAX_RESPONSE_TOKENS,
            "messages",   messages
        );
        var rc  = restClientBuilder.baseUrl(baseUrl).build();
        var raw = rc.post()
            .uri("/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .body(objectMapper.writeValueAsString(body))
            .retrieve()
            .body(String.class);
        return objectMapper.readTree(raw)
                .path("choices").path(0).path("message").path("content").asText("");
    }

    private List<Map<String, String>> buildHistory(ChatSession session) {
        var out   = new ArrayList<Map<String, String>>();
        var msgs  = session.getMessages();
        int start = Math.max(0, msgs.size() - MAX_HISTORY_PAIRS * 2);
        for (int i = start; i < msgs.size(); i++) {
            var m = msgs.get(i);
            out.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        return out;
    }

    private void trimHistory(ChatSession session) {
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

    private void saveSession(ChatSession session) {
        try {
            redis.opsForValue().set(SESSION_KEY + session.getId(),
                objectMapper.writeValueAsString(session), SESSION_TTL_H, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error saving chat session: {}", e.getMessage());
        }
    }

    private ChatSession loadSession(String sessionId) {
        try {
            var raw = redis.opsForValue().get(SESSION_KEY + sessionId);
            if (raw == null) return null;
            return objectMapper.readValue(raw, ChatSession.class);
        } catch (Exception e) {
            log.error("Error loading chat session: {}", e.getMessage());
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
                "Massa sessions creades. Torna-ho a provar en una hora.");
        }
    }

    private void checkMessageRateLimit(String ip) {
        var raw = redis.opsForValue().get(RATE_MSG_KEY + ip);
        if (raw != null && Long.parseLong(raw) >= MAX_MSGS_PER_IP_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Massa missatges enviats. Torna-ho a provar en una hora.");
        }
    }

    private LandingChatContext loadContext(String landingId) {
        if (landingId == null) return null;
        try {
            return chatContextRepository.findById(UUID.fromString(landingId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private LandingChatContext buildGenericContext(UUID landingId, String title) {
        return LandingChatContext.builder()
                .landingId(landingId)
                .businessName(title)
                .systemPrompt(buildGenericSystemPrompt(title))
                .build();
    }

    private String buildGenericSystemPrompt(String name) {
        return "Ets l'assistent virtual de " + name + ". " +
               "Respon sempre en català de forma amigable, concisa i professional. " +
               "Ajuda als visitants amb informació sobre el negoci, serveis i com contactar-hi. " +
               "Si no saps la resposta, convida'ls a trucar o escriure per correu. " +
               "Respostes curtes (màx. 2-3 frases), estil WhatsApp.";
    }
}
