package com.amg.digitalitzacio.chat.application;

import com.amg.digitalitzacio.agents.application.ChannelUsageService;
import com.amg.digitalitzacio.chat.domain.ChatSession;
import com.amg.digitalitzacio.chat.domain.LandingChatContext;
import com.amg.digitalitzacio.chat.domain.LandingChatContextRepository;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private static final String CHAT_MODEL           = "claude-haiku-4-5-20251001";
    private static final int    MAX_RESPONSE_TOKENS  = 300;
    private static final int    MAX_HISTORY_PAIRS    = 10;
    private static final int    MAX_INPUT_CHARS      = 500;
    private static final String ANTHROPIC_BASE       = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION    = "2023-06-01";

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
    private final SystemConfigService sysConfig;
    private final RestClient.Builder restClientBuilder;
    private final ChannelUsageService channelUsageService;

    @Value("${app.landing.base-domain:webs.amgdl.com}")
    private String landingBaseDomain;

    public record CreateSessionResult(String sessionId, String greeting) {}
    public record SendMessageResult(String sessionId, String reply, boolean terminated) {}

    public CreateSessionResult createSession(String landingSlug, String ip) {
        checkSessionRateLimit(ip);

        var landing = landingRepository.findBySlug(landingSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Landing not found"));

        var ctx = chatContextRepository.findById(landing.getId())
                .orElseGet(() -> buildGenericContext(landing.getId(), landing.getTitle()));

        var sessionId = UUID.randomUUID().toString();
        var greeting  = generateGreeting(ctx);

        var session = ChatSession.builder()
                .id(sessionId)
                .landingSlug(landingSlug)
                .landingId(landing.getId().toString())
                .messageCount(0)
                .build();

        session.getMessages().add(new ChatSession.ChatMessage("assistant", greeting));
        saveSession(session);
        incrementRateCounter(RATE_SESS_KEY + ip, 3600);

        return new CreateSessionResult(sessionId, greeting);
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
        String systemPrompt = ctx != null ? ctx.getSystemPrompt() : buildGenericSystemPrompt(session.getLandingSlug());

        var history = buildHistory(session);
        String reply = callClaude(systemPrompt, history, userMessage);
        if (reply == null || reply.isBlank()) {
            reply = "Ho sent, en aquest moment no puc respondre. Torna-ho a provar en uns instants.";
        }

        session.getMessages().add(new ChatSession.ChatMessage("user", userMessage));
        session.getMessages().add(new ChatSession.ChatMessage("assistant", reply));
        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastActivityAt(Instant.now());
        trimHistory(session);
        saveSession(session);
        incrementRateCounter(RATE_MSG_KEY + ip, 3600);

        // Registra l'ús del canal de xat (best-effort, no bloca la resposta)
        try {
            landingRepository.findById(UUID.fromString(session.getLandingId()))
                    .ifPresent(l -> channelUsageService.record(l.getTenantId(), ChannelUsageService.CHAT));
        } catch (Exception ignored) {}

        return new SendMessageResult(sessionId, reply, false);
    }

    // --- Claude ---

    private String generateGreeting(LandingChatContext ctx) {
        String fallback = "Hola! Sóc l'assistent virtual de " + ctx.getBusinessName() + ". En què puc ajudar-te?";
        try {
            String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return fallback;
            var result = callAnthropicApi(apiKey,
                ctx.getSystemPrompt() + "\n\nGenera un missatge de benvinguda breu i amigable en català (màx. 2 frases). Presenta't pel nom del negoci i ofereix ajuda.",
                List.of(),
                "Hola");
            return (result != null && !result.isBlank()) ? result : fallback;
        } catch (Exception e) {
            log.warn("Could not generate greeting: {}", e.getMessage());
            return fallback;
        }
    }

    private String callClaude(String systemPrompt, List<Map<String, String>> history, String userMessage) {
        try {
            String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return null;
            return callAnthropicApi(apiKey, systemPrompt, history, userMessage);
        } catch (Exception e) {
            log.error("Claude API error in chat: {}", e.getMessage());
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
