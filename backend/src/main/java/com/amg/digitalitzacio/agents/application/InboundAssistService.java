package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.AgentMode;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Inbound Assist (Spec 59) — per als contactes entrants d'un tenant (correu, formulari,
 * WhatsApp, xat), l'agent IA redacta un esborrany i el tenant l'aprova des del SEU Telegram
 * (✅ Enviar / 🔄 Demana canvis / ✍️ L'escric jo) abans d'enviar res.
 *
 * Fase 1: mode HYBRID + canal EMAIL (correu i formulari; la resposta surt per email).
 * Replica el patró de Redis de SocialDmService, però amb enviament channel-aware.
 * NO modifica SocialDmService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InboundAssistService {

    private static final String CTX_KEY = "ia:ctx:%s";       // id → context (JSON)
    private static final String AWAIT_KEY = "ia:await:%s";   // chatId → "<id>|REFINE|MANUAL"
    private static final int TTL_HOURS = 24;

    private final TenantChatLinkRepository chatLinkRepository;
    private final ConversationalAgentService agentService;
    private final ConversationService conversationService;
    private final TenantAIConfigRepository aiConfigRepository;
    private final TelegramBotClient telegramBotClient;
    private final EmailChannel emailChannel;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final AIProviderRouter aiRouter;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * Intenta gestionar un contacte entrant amb esborrany + aprovació.
     * @return true si l'ha gestionat (tenant HYBRID amb Telegram); false → que segueixi el flux actual.
     */
    public boolean tryIntake(UUID tenantId, ConversationChannel channel, String fromRef,
                             String fromName, String inboundText) {
        if (inboundText == null || inboundText.isBlank()) return false;
        TenantChatLink link = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        if (link == null || Boolean.FALSE.equals(link.getIsActive())) return false;
        if (link.modeFor(channel) != AgentMode.HYBRID) return false; // AUTO/MANUAL → flux actual
        Long chatId = link.getTelegramChatId();
        if (chatId == null) return false;

        String draft = agentService.generateDmDraft(tenantId, fromRef, channel, inboundText);
        if (draft == null || draft.isBlank()) return false;

        // Número emissor WhatsApp del tenant (per poder enviar la resposta pel mateix canal)
        String senderId = switch (channel) {
            case WHATSAPP_META -> link.getWhatsappMetaPhoneNumberId();
            case WHATSAPP      -> link.getWhatsappPhoneNumber();
            default            -> null;
        };

        String id = UUID.randomUUID().toString().substring(0, 8);
        storeContext(id, tenantId, chatId, channel, fromRef, inboundText, draft, senderId);
        sendApproval(chatId, id, channel, fromRef, inboundText, draft);
        return true;
    }

    private void sendApproval(Long chatId, String id, ConversationChannel channel,
                             String fromRef, String inboundText, String draft) {
        String text = "📨 <b>Nova consulta · " + channelLabel(channel) + "</b>\n"
                + "👤 " + escapeHtml(fromRef) + "\n"
                + "\"" + escapeHtml(clip(inboundText, 400)) + "\"\n\n"
                + "🤖 <b>Resposta proposada:</b>\n" + escapeHtml(clip(draft, 700));
        var buttons = List.of(
                Map.of("text", "✅ Enviar", "callback_data", "iaok:" + id),
                Map.of("text", "🔄 Demana canvis", "callback_data", "iarf:" + id),
                Map.of("text", "✍️ L'escric jo", "callback_data", "iawr:" + id)
        );
        telegramBotClient.sendMessageWithButtons(chatId, text, buttons);
    }

    // ─────────────── Callbacks ───────────────

    /** ✅ Enviar → envia l'esborrany pel canal. */
    public String approve(Long chatId, String id) {
        ObjectNode ctx = loadContext(id);
        if (ctx == null) return "⚠️ La consulta ha caducat.";
        if (!owns(ctx, chatId)) return "⚠️ No accessible des d'aquest xat.";
        String draft = ctx.path("draft").asText(null);
        if (draft == null || draft.isBlank()) return "⚠️ No hi ha cap esborrany.";
        String result = send(ctx, draft);
        redis.delete(CTX_KEY.formatted(id));
        return result;
    }

    /** 🔄 Demana canvis → espera la indicació de reescriptura. */
    public void startRefine(Long chatId, String id) {
        ObjectNode ctx = loadContext(id);
        if (ctx == null || !owns(ctx, chatId)) return;
        redis.opsForValue().set(AWAIT_KEY.formatted(chatId), id + "|REFINE", TTL_HOURS, TimeUnit.HOURS);
    }

    /** ✍️ L'escric jo → espera la resposta final manual. */
    public void startManual(Long chatId, String id) {
        ObjectNode ctx = loadContext(id);
        if (ctx == null || !owns(ctx, chatId)) return;
        redis.opsForValue().set(AWAIT_KEY.formatted(chatId), id + "|MANUAL", TTL_HOURS, TimeUnit.HOURS);
    }

    public boolean hasAwait(Long chatId) {
        return Boolean.TRUE.equals(redis.hasKey(AWAIT_KEY.formatted(chatId)));
    }

    /** El següent text del tenant: instrucció de refinament o resposta final. */
    public String submitAwaitText(Long chatId, String text) {
        String key = AWAIT_KEY.formatted(chatId);
        String raw = redis.opsForValue().get(key);
        if (raw == null) return null;
        redis.delete(key);
        String[] parts = raw.split("\\|", 2);
        String id = parts[0];
        String mode = parts.length > 1 ? parts[1] : "MANUAL";
        ObjectNode ctx = loadContext(id);
        if (ctx == null) return "⚠️ La consulta ha caducat.";
        if (!owns(ctx, chatId)) return "⚠️ No accessible des d'aquest xat.";

        if ("REFINE".equals(mode)) {
            String newDraft = refine(ctx.path("inboundText").asText(""), ctx.path("draft").asText(""), text);
            ctx.put("draft", newDraft);
            try {
                redis.opsForValue().set(CTX_KEY.formatted(id), objectMapper.writeValueAsString(ctx), TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception ignore) { }
            ConversationChannel channel = ConversationChannel.valueOf(ctx.path("channel").asText());
            sendApproval(chatId, id, channel, ctx.path("fromRef").asText(""), ctx.path("inboundText").asText(""), newDraft);
            return null; // ja s'ha enviat el nou esborrany amb botons
        }
        // MANUAL
        String result = send(ctx, text);
        redis.delete(CTX_KEY.formatted(id));
        return result;
    }

    // ─────────────── Interns ───────────────

    /** Reescriu l'esborrany segons la indicació de l'operador. */
    private String refine(String inbound, String currentDraft, String instruction) {
        String system = """
                Ets l'assistent de comunicació d'un negoci. Reescriu la resposta segons la
                indicació de l'operador, mantenint el context i un to professional i proper.
                Respon NOMÉS amb la resposta reescrita, sense preàmbuls.
                """;
        String user = "Missatge del client: " + inbound + "\n\nResposta actual: " + currentDraft
                + "\n\nIndicació de l'operador: " + instruction + "\n\nResposta reescrita:";
        try {
            String out = aiRouter.forModel(aiRouter.defaultModel()).chat(system, List.of(), user);
            return (out != null && !out.isBlank()) ? out.trim() : currentDraft;
        } catch (Exception e) {
            log.warn("Refinament IA fallit: {}", e.getMessage());
            return currentDraft;
        }
    }

    /** Enviament channel-aware. Fase 1: EMAIL. */
    private String send(ObjectNode ctx, String text) {
        ConversationChannel channel = ConversationChannel.valueOf(ctx.path("channel").asText());
        String fromRef = ctx.path("fromRef").asText();
        String senderId = ctx.path("senderId").asText("");
        return switch (channel) {
            case EMAIL -> {
                try {
                    // Mateix enviament que el flux AUTO: sender del tenant + reply-to a l'adreça
                    // d'inbound (amg@inbound.amgdl.com) perquè el fil continuï atès pel bot.
                    UUID tid = UUID.fromString(ctx.path("tenantId").asText());
                    TenantAIConfig cfg = aiConfigRepository.findById(tid).orElse(TenantAIConfig.defaultFor(tid));
                    emailChannel.sendMessage(fromRef, "Resposta a la teva consulta", text,
                            cfg.getSenderEmail(), cfg.getSenderName(), cfg.getReplyToEmail());
                    finalizeInbox(ctx, channel, fromRef, text);
                    yield "✅ Resposta enviada per email a " + fromRef + ".";
                } catch (Exception e) {
                    log.error("Error enviant email a {}: {}", fromRef, e.getMessage());
                    yield "⚠️ No s'ha pogut enviar l'email.";
                }
            }
            case WHATSAPP -> {
                if (senderId.isBlank()) yield noWabaFallback(fromRef, text);
                try {
                    whatsAppChannel.sendMessage(senderId, fromRef, text);
                    finalizeInbox(ctx, channel, fromRef, text);
                    yield "✅ Resposta enviada per WhatsApp a " + fromRef + ".";
                } catch (Exception e) {
                    log.error("Error enviant WhatsApp a {}: {}", fromRef, e.getMessage());
                    yield "⚠️ No s'ha pogut enviar el WhatsApp.";
                }
            }
            case WHATSAPP_META -> {
                if (senderId.isBlank()) yield noWabaFallback(fromRef, text);
                try {
                    whatsAppMetaChannel.sendMessage(senderId, fromRef, text);
                    finalizeInbox(ctx, channel, fromRef, text);
                    yield "✅ Resposta enviada per WhatsApp a " + fromRef + ".";
                } catch (Exception e) {
                    log.error("Error enviant WhatsApp Meta a {}: {}", fromRef, e.getMessage());
                    yield "⚠️ No s'ha pogut enviar el WhatsApp.";
                }
            }
            default -> "⚠️ Canal encara no suportat: " + channel; // Widget → fase següent
        };
    }

    /**
     * Sense WABA configurada no es pot enviar per API. Es dona el text i un enllaç wa.me
     * perquè el tenant respongui a mà des del seu WhatsApp. No es finalitza l'Inbox
     * (l'esborrany queda pendent fins que es contesti de veritat).
     */
    private String noWabaFallback(String toPhone, String text) {
        String digits = toPhone == null ? "" : toPhone.replaceAll("[^0-9]", "");
        String link = digits.isBlank() ? "" : "\n👉 https://wa.me/" + digits;
        return "ℹ️ WhatsApp sense WABA connectada: copia i envia des del teu WhatsApp:" + link
                + "\n\n" + text;
    }

    /** Deixa el panell central (Inbox) reflectint el text realment enviat, no l'esborrany. */
    private void finalizeInbox(ObjectNode ctx, ConversationChannel channel, String fromRef, String sentText) {
        try {
            UUID tenantId = UUID.fromString(ctx.path("tenantId").asText());
            conversationService.finalizeSentReply(tenantId, fromRef, channel, sentText);
        } catch (Exception e) {
            log.warn("No s'ha pogut finalitzar l'esborrany a l'Inbox (no crític): {}", e.getMessage());
        }
    }

    private boolean owns(ObjectNode ctx, Long chatId) {
        return chatId != null && ctx.path("chatId").asLong(0) == chatId;
    }

    private void storeContext(String id, UUID tenantId, Long chatId, ConversationChannel channel,
                              String fromRef, String inboundText, String draft, String senderId) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("tenantId", tenantId.toString());
        node.put("chatId", chatId);
        node.put("channel", channel.name());
        node.put("fromRef", fromRef);
        node.put("inboundText", inboundText);
        node.put("draft", draft);
        if (senderId != null) node.put("senderId", senderId);
        try {
            redis.opsForValue().set(CTX_KEY.formatted(id), objectMapper.writeValueAsString(node), TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Error desant context inbound: {}", e.getMessage());
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

    private String channelLabel(ConversationChannel c) {
        return switch (c) {
            case EMAIL -> "Correu";
            case WHATSAPP_META, WHATSAPP -> "WhatsApp";
            case WIDGET -> "Xat web";
            default -> c.name();
        };
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
