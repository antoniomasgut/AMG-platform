package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * DMs d'Instagram i Messenger → Omnichannel Inbox amb aprovació híbrida (Mòdul 56 F2).
 *
 * Flux: DM entrant → l'agent IA prepara una resposta (persistida a l'Inbox com a
 * pendingApproval) → s'avisa el tenant per Telegram amb l'esborrany → el tenant
 * aprova (s'envia via Graph API) o escriu la seva pròpia resposta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialDmService {

    private static final String CTX_KEY = "dm:ctx:%s";        // id curt → context del DM (JSON)
    private static final String PENDING_KEY = "dm:pending:%s"; // chatId → id (esperant text propi)
    private static final int TTL_HOURS = 24;

    private final SocialMetaConfigRepository metaConfigRepo;
    private final SocialFeatureService featureService;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TelegramBotClient telegramBotClient;
    private final ConversationalAgentService agentService;
    private final MetaMessagingChannel messagingChannel;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /** DM entrant de Messenger (webhook camp messaging) */
    @Async
    public void handleMessengerDm(String pageId, String senderId, String senderName, String text) {
        var meta = metaConfigRepo.findByFacebookPageId(pageId).orElse(null);
        if (meta == null) return;
        process(meta, ConversationChannel.MESSENGER, "Messenger", senderId, senderName, text);
    }

    /** DM entrant d'Instagram (webhook camp messages) */
    @Async
    public void handleInstagramDm(String igAccountId, String senderId, String senderName, String text) {
        var meta = metaConfigRepo.findByInstagramAccountId(igAccountId).orElse(null);
        if (meta == null) return;
        process(meta, ConversationChannel.INSTAGRAM, "Instagram", senderId, senderName, text);
    }

    private void process(SocialMetaConfig meta, ConversationChannel channel, String channelLabel,
                         String senderId, String senderName, String text) {
        if (senderId == null || text == null || text.isBlank()) return;

        var tenantId = meta.getTenantId();
        // Ignora els ecos de la mateixa pàgina/compte
        if (senderId.equals(meta.getFacebookPageId()) || senderId.equals(meta.getInstagramAccountId())) return;

        if (!featureService.get(tenantId).dmsToInbox()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        Long chatId = chatLink != null ? chatLink.getTelegramChatId() : null;

        // L'agent IA prepara la resposta i la persista a l'Inbox (pendingApproval)
        String draft = agentService.generateDmDraft(tenantId, senderId, channel, text);

        if (chatId == null) {
            // Sense Telegram enllaçat: la conversa queda a l'Inbox per respondre des del portal
            return;
        }

        String id = UUID.randomUUID().toString().substring(0, 8);
        storeContext(id, tenantId, chatId, channel, senderId, draft);

        var sb = new StringBuilder();
        sb.append("✉️ <b>Nou missatge directe · ").append(channelLabel).append("</b>\n");
        if (senderName != null && !senderName.isBlank()) {
            sb.append("👤 ").append(escapeHtml(senderName)).append("\n");
        }
        sb.append("\"").append(escapeHtml(clip(text, 400))).append("\"\n");

        if (draft != null && !draft.isBlank()) {
            sb.append("\n🤖 <b>Resposta preparada:</b>\n").append(escapeHtml(clip(draft, 600)));
            sb.append("\n\nToca <b>✅ Enviar</b> per enviar-la, o <b>✍️ Escriure</b> per redactar la teva.");
            var buttons = List.of(
                Map.of("text", "✅ Enviar", "callback_data", "dmok:" + id),
                Map.of("text", "✍️ Escriure", "callback_data", "dmwr:" + id)
            );
            telegramBotClient.sendMessageWithButtons(chatId, sb.toString(), buttons);
        } else {
            sb.append("\nToca <b>✍️ Respondre</b> per contestar des d'aquí.");
            var buttons = List.of(
                Map.of("text", "✍️ Respondre", "callback_data", "dmwr:" + id)
            );
            telegramBotClient.sendMessageWithButtons(chatId, sb.toString(), buttons);
        }
    }

    /** El tenant aprova l'esborrany → s'envia el DM via Graph API */
    public String approveDraft(Long chatId, String id) {
        var ctx = loadContext(id);
        if (ctx == null) return "⚠️ El missatge ha caducat.";
        if (!ownsContext(ctx, chatId)) return "⚠️ Aquest missatge no és accessible des d'aquest xat.";
        String draft = ctx.path("draft").asText(null);
        if (draft == null || draft.isBlank()) return "⚠️ No hi ha cap esborrany per enviar.";
        return send(ctx, draft);
    }

    /** El tenant vol escriure la seva pròpia resposta → activa estat pendent */
    public void startManualReply(Long chatId, String id) {
        var ctx = loadContext(id);
        // Només el xat propietari del context pot iniciar la resposta manual
        if (ctx == null || !ownsContext(ctx, chatId)) return;
        redis.opsForValue().set(PENDING_KEY.formatted(chatId), id, TTL_HOURS, TimeUnit.HOURS);
    }

    public boolean hasPending(Long chatId) {
        return Boolean.TRUE.equals(redis.hasKey(PENDING_KEY.formatted(chatId)));
    }

    /** El següent text del tenant es pren com a resposta manual i s'envia */
    public String submitManualReply(Long chatId, String text) {
        String key = PENDING_KEY.formatted(chatId);
        String id = redis.opsForValue().get(key);
        if (id == null) return null;
        redis.delete(key);
        var ctx = loadContext(id);
        if (ctx == null) return "⚠️ El missatge ha caducat.";
        // El PENDING_KEY ja està lligat a aquest chatId, però revalidem el context per coherència
        if (!ownsContext(ctx, chatId)) return "⚠️ Aquest missatge no és accessible des d'aquest xat.";
        return send(ctx, text);
    }

    /** El context de DM només és accionable pel xat de Telegram del tenant propietari */
    private boolean ownsContext(ObjectNode ctx, Long chatId) {
        if (chatId == null) return false;
        long ownerChatId = ctx.path("chatId").asLong(0);
        return ownerChatId != 0 && ownerChatId == chatId;
    }

    private String send(ObjectNode ctx, String text) {
        UUID tenantId = UUID.fromString(ctx.path("tenantId").asText());
        String recipientId = ctx.path("recipientId").asText();
        boolean ok = messagingChannel.sendMessage(tenantId, recipientId, text);
        return ok ? "✅ Missatge enviat." : "⚠️ No s'ha pogut enviar el missatge.";
    }

    private void storeContext(String id, UUID tenantId, Long chatId, ConversationChannel channel, String recipientId, String draft) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("tenantId", tenantId.toString());
        node.put("chatId", chatId);
        node.put("channel", channel.name());
        node.put("recipientId", recipientId);
        if (draft != null) node.put("draft", draft);
        try {
            redis.opsForValue().set(CTX_KEY.formatted(id), objectMapper.writeValueAsString(node), TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Error desant context DM: {}", e.getMessage());
        }
    }

    private ObjectNode loadContext(String id) {
        String json = redis.opsForValue().get(CTX_KEY.formatted(id));
        if (json == null) return null;
        try {
            return (ObjectNode) objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
