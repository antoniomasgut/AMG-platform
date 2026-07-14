package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.AbsenceRescheduleService;
import com.amg.digitalitzacio.agents.application.AgentRegistry;
import com.amg.digitalitzacio.agents.application.AmgAdminCommandService;
import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.application.InboundAssistService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.SpeechToTextService;
import com.amg.digitalitzacio.agents.application.TeamGrowthService;
import com.amg.digitalitzacio.agents.application.TenantAgendaQueryService;
import com.amg.digitalitzacio.agents.application.TenantTelegramCommandService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.documents.delivery.application.BudgetWorkflowService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.social.application.SocialPublisherOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookController {

    private final TenantChatLinkRepository chatLinkRepository;
    private final AgentRegistry agentRegistry;
    private final ConversationalAgentService conversationalAgentService;
    private final TeamGrowthService teamGrowthService;
    private final AbsenceRescheduleService absenceRescheduleService;
    private final BudgetWorkflowService budgetWorkflowService;
    private final AmgAdminCommandService amgAdminCommandService;
    private final SystemConfigService systemConfigService;
    private final SocialPublisherOrchestrator socialOrchestrator;
    private final com.amg.digitalitzacio.content.application.ContentPlannerFlowService contentPlannerFlow;
    private final InboundAssistService inboundAssistService;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final SpeechToTextService speechToTextService;
    private final TenantAgendaQueryService tenantAgendaQueryService;
    private final TenantTelegramCommandService tenantCommandService;
    private final com.amg.digitalitzacio.agents.application.TelegramBotClient telegramBotClient;
    private final com.amg.digitalitzacio.google.application.GoogleReviewReplyService reviewReplyService;
    private final com.amg.digitalitzacio.social.application.ReviewSocialShareService reviewSocialShareService;
    private final com.amg.digitalitzacio.social.application.SocialCommentReplyService commentReplyService;
    private final com.amg.digitalitzacio.social.application.SocialDmService socialDmService;

    private static final java.util.regex.Pattern SOCIAL_TRIGGER = java.util.regex.Pattern.compile(
        "(?i)\\b(publica|publicar|post|instagram|facebook|penja|penjar|xarxes)\\b");

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
            @RequestBody Map<String, Object> payload) {
        String expectedSecret = systemConfigService.get("TELEGRAM_WEBHOOK_SECRET");
        if (expectedSecret != null && !expectedSecret.isBlank()) {
            if (!expectedSecret.equals(secretToken)) {
                log.warn("Telegram webhook: secret_token invàlid o absent");
                return ResponseEntity.status(401).body("Unauthorized");
            }
        }
        try {
            // Callback dels botons inline (p. ex. "✓ Contactat" al xat de vendes)
            if (payload.get("callback_query") instanceof Map<?, ?> callback) {
                String callbackId = callback.get("id") instanceof String cid ? cid : null;
                String data = callback.get("data") instanceof String d ? d : "";
                Long cbChatId = null;
                if (callback.get("message") instanceof Map<?, ?> cbMsg
                        && cbMsg.get("chat") instanceof Map<?, ?> cbChat
                        && cbChat.get("id") instanceof Number cbId) {
                    cbChatId = cbId.longValue();
                }
                // Botó "📢 Crear aquest post" d'un suggeriment social (Mòdul 55)
                if (cbChatId != null && "sugg:new".equals(data)) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Preparant...");
                        }
                        socialOrchestrator.startFlow(link.get().getTenantId(), cbChatId);
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "✍️ Respondre" d'una ressenya de Google (Mòdul 54)
                if (cbChatId != null && data.startsWith("grev:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String reviewId = data.substring("grev:".length());
                        reviewReplyService.startReply(cbChatId, reviewId);
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Escriu la resposta");
                        }
                        telegramBotClient.sendMessage(cbChatId,
                                "✍️ Escriu la resposta que vols publicar a aquesta ressenya de Google:");
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "🤖 Suggerir resposta" d'una ressenya (Mòdul 56 F1)
                if (cbChatId != null && data.startsWith("grevai:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String reviewId = data.substring("grevai:".length());
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Generant resposta...");
                        }
                        var draft = reviewReplyService.generateAndStoreDraft(cbChatId, link.get().getTenantId(), reviewId);
                        if (draft != null) {
                            var btn = Map.of("text", "✅ Publicar", "callback_data", "grevpub:" + reviewId);
                            telegramBotClient.sendMessageWithButtons(cbChatId,
                                    "🤖 <b>Resposta suggerida</b>\n\n" + draft
                                    + "\n\nPublica-la o escriu-ne una de teva.", List.of(btn));
                        } else {
                            telegramBotClient.sendMessage(cbChatId, "⚠️ No he pogut generar una resposta ara mateix.");
                        }
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "✅ Publicar" de l'esborrany IA d'una ressenya (Mòdul 56 F1)
                if (cbChatId != null && data.startsWith("grevpub:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String reviewId = data.substring("grevpub:".length());
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Publicant...");
                        }
                        var msg = reviewReplyService.publishDraft(link.get().getTenantId(), cbChatId, reviewId);
                        telegramBotClient.sendMessage(cbChatId, msg);
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "✍️ Respondre" d'un comentari de Facebook (Mòdul 55, feature 1)
                if (cbChatId != null && data.startsWith("cmt:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String commentId = data.substring("cmt:".length());
                        commentReplyService.startReply(cbChatId, commentId);
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Escriu la resposta");
                        }
                        telegramBotClient.sendMessage(cbChatId,
                                "✍️ Escriu la resposta que vols publicar a aquest comentari:");
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "🤖 Suggerir resposta" d'un comentari (Mòdul 56 F1)
                if (cbChatId != null && data.startsWith("cmtai:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String commentId = data.substring("cmtai:".length());
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Generant resposta...");
                        }
                        var draft = commentReplyService.generateAndStoreDraft(cbChatId, commentId);
                        if (draft != null) {
                            var btn = Map.of("text", "✅ Publicar", "callback_data", "cmtpub:" + commentId);
                            telegramBotClient.sendMessageWithButtons(cbChatId,
                                    "🤖 <b>Resposta suggerida</b>\n\n" + draft
                                    + "\n\nPublica-la o escriu-ne una de teva.", List.of(btn));
                        } else {
                            telegramBotClient.sendMessage(cbChatId, "⚠️ No he pogut generar una resposta ara mateix.");
                        }
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "✅ Publicar" de l'esborrany IA d'un comentari (Mòdul 56 F1)
                if (cbChatId != null && data.startsWith("cmtpub:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String commentId = data.substring("cmtpub:".length());
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Publicant...");
                        }
                        var msg = commentReplyService.publishDraft(link.get().getTenantId(), cbChatId, commentId);
                        telegramBotClient.sendMessage(cbChatId, msg);
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "📢 Compartir a xarxes" d'una ressenya 5★ (Mòdul 55, feature 4)
                if (cbChatId != null && data.startsWith("gshare:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String reviewId = data.substring("gshare:".length());
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Preparant el post...");
                        }
                        reviewSocialShareService.preview(link.get().getTenantId(), cbChatId, reviewId);
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "✅ Publicar" del post generat des d'una ressenya (Mòdul 55, feature 4)
                if (cbChatId != null && data.startsWith("gpub:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        try {
                            var postId = java.util.UUID.fromString(data.substring("gpub:".length()));
                            if (callbackId != null) {
                                telegramBotClient.answerCallbackQuery(callbackId, "Publicant...");
                            }
                            reviewSocialShareService.publish(link.get().getTenantId(), cbChatId, postId);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "✅ Enviar" d'un DM (Mòdul 56 F2): aprova i envia l'esborrany IA
                if (cbChatId != null && data.startsWith("dmok:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String dmId = data.substring("dmok:".length());
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Enviant...");
                        }
                        String msg = socialDmService.approveDraft(cbChatId, dmId);
                        telegramBotClient.sendMessage(cbChatId, msg);
                    }
                    return ResponseEntity.ok("ok");
                }

                // Botó "✍️ Escriure/Respondre" d'un DM (Mòdul 56 F2): activa resposta manual
                if (cbChatId != null && data.startsWith("dmwr:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        String dmId = data.substring("dmwr:".length());
                        socialDmService.startManualReply(cbChatId, dmId);
                        if (callbackId != null) {
                            telegramBotClient.answerCallbackQuery(callbackId, "Escriu la resposta");
                        }
                        telegramBotClient.sendMessage(cbChatId, "✍️ Escriu la teva resposta i l'enviaré al client.");
                    }
                    return ResponseEntity.ok("ok");
                }

                // Content Planner (Mòdul 58): confirmar / refer el post preparat des del brief
                if (cbChatId != null && data.startsWith("cpok:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        if (callbackId != null) telegramBotClient.answerCallbackQuery(callbackId, "Publicant...");
                        contentPlannerFlow.approve(cbChatId, link.get().getTenantId(),
                                java.util.UUID.fromString(data.substring("cpok:".length())));
                    }
                    return ResponseEntity.ok("ok");
                }
                if (cbChatId != null && data.startsWith("cpwr:")) {
                    var link = chatLinkRepository.findByTelegramChatId(cbChatId);
                    if (link.isPresent() && link.get().getIsActive()) {
                        if (callbackId != null) telegramBotClient.answerCallbackQuery(callbackId, "Refent el text...");
                        contentPlannerFlow.rewrite(cbChatId, link.get().getTenantId(),
                                java.util.UUID.fromString(data.substring("cpwr:".length())));
                    }
                    return ResponseEntity.ok("ok");
                }

                // Inbound Assist (Mòdul 59): aprovar / demanar canvis / escriure la resposta
                if (cbChatId != null && data.startsWith("iaok:")) {
                    if (callbackId != null) telegramBotClient.answerCallbackQuery(callbackId, "Enviant...");
                    String msg = inboundAssistService.approve(cbChatId, data.substring("iaok:".length()));
                    telegramBotClient.sendMessage(cbChatId, msg);
                    return ResponseEntity.ok("ok");
                }
                if (cbChatId != null && data.startsWith("iarf:")) {
                    inboundAssistService.startRefine(cbChatId, data.substring("iarf:".length()));
                    if (callbackId != null) telegramBotClient.answerCallbackQuery(callbackId, "Escriu la indicació");
                    telegramBotClient.sendMessage(cbChatId, "✍️ Escriu com vols que la canviï (ex: «més curta», «en castellà»).");
                    return ResponseEntity.ok("ok");
                }
                if (cbChatId != null && data.startsWith("iawr:")) {
                    inboundAssistService.startManual(cbChatId, data.substring("iawr:".length()));
                    if (callbackId != null) telegramBotClient.answerCallbackQuery(callbackId, "Escriu la resposta");
                    telegramBotClient.sendMessage(cbChatId, "✍️ Escriu la teva resposta i l'enviaré.");
                    return ResponseEntity.ok("ok");
                }

                if (cbChatId != null && callbackId != null && amgAdminCommandService.isAdminChat(cbChatId)) {
                    amgAdminCommandService.handleCallback(cbChatId, data, callbackId);
                }
                return ResponseEntity.ok("ok");
            }

            var message = extractMessage(payload);
            if (message == null) {
                return ResponseEntity.ok("ok");
            }

            var chatId = message.get("chat") instanceof Map<?, ?> chat
                    ? ((Number) chat.get("id")).longValue()
                    : null;
            var text = message.get("text") instanceof String t ? t.trim() : "";

            // Detecta foto enviada directament (per al flux social publisher)
            String photoFileId = null;
            if (message.get("photo") instanceof java.util.List<?> photos && !photos.isEmpty()) {
                // Agafem la foto de major resolució (última de la llista)
                var lastPhoto = photos.get(photos.size() - 1);
                if (lastPhoto instanceof Map<?, ?> photoMap) {
                    photoFileId = photoMap.get("file_id") instanceof String fid ? fid : null;
                }
                // Les fotos de TG no porten "text" sinó "caption"
                if (text.isBlank() && message.get("caption") instanceof String cap) {
                    text = cap.trim();
                }
            }
            // Nota de veu (F5): transcriure-la i tractar-la com a text
            String voiceFileId = null;
            if (message.get("voice") instanceof Map<?, ?> voice) {
                voiceFileId = voice.get("file_id") instanceof String vf ? vf : null;
            } else if (message.get("audio") instanceof Map<?, ?> audio) {
                voiceFileId = audio.get("file_id") instanceof String af ? af : null;
            }

            Long fromUserId = null;
            String firstName = "Usuari";
            if (message.get("from") instanceof Map<?, ?> from) {
                firstName = from.get("first_name") instanceof String fn ? fn : "Usuari";
                fromUserId = from.get("id") instanceof Number uid ? uid.longValue() : null;
            }

            if (chatId == null) {
                return ResponseEntity.ok("ok");
            }

            if (voiceFileId != null && text.isBlank()) {
                if (!speechToTextService.isConfigured()) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            "🎙 He rebut la teva nota de veu, però la transcripció encara no està activada. Escriu-me el missatge en text, si us plau."));
                }
                var transcription = speechToTextService.transcribeTelegramVoice(voiceFileId);
                if (transcription.isEmpty()) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            "🎙 No he pogut entendre la nota de veu — prova de nou o escriu-m'ho en text."));
                }
                text = transcription.get();
                log.info("Nota de veu transcrita per chat {}: {} chars", chatId, text.length());
            }

            log.info("Missatge TG rebut de chat {}: {}", chatId, text);

            // Inbound Assist (Mòdul 59): resposta/indicació pendent. ABANS del catch-all admin,
            // perquè el Telegram del tenant AMG sol ser el xat de vendes (admin chat) i s'engoliria.
            if (!text.startsWith("/") && inboundAssistService.hasAwait(chatId)) {
                String reply = inboundAssistService.submitAwaitText(chatId, text);
                if (reply != null) telegramBotClient.sendMessage(chatId, reply);
                return ResponseEntity.ok("ok");
            }

            // Comandes AMG Admin (xat de vendes o infraops)
            if (amgAdminCommandService.isAdminChat(chatId)) {
                amgAdminCommandService.handle(chatId, text);
                return ResponseEntity.ok("ok");
            }

            // Handle /start with link code
            if (text.startsWith("/start")) {
                var parts = text.split("\\s+");
                if (parts.length > 1) {
                    var linkCode = parts[1];
                    var linkOpt = chatLinkRepository.findByLinkCode(linkCode);
                    if (linkOpt.isPresent() && linkOpt.get().getIsActive()
                            && linkOpt.get().getLinkCodeExpiresAt() != null
                            && linkOpt.get().getLinkCodeExpiresAt().isAfter(Instant.now())) {
                        var link = linkOpt.get();
                        link.setTelegramChatId(chatId);
                        link.setLinkCode(null);
                        link.setLinkCodeExpiresAt(null);
                        chatLinkRepository.save(link);

                        log.info("Chat TG {} vinculat al tenant {}", chatId, link.getTenantId());
                        return ResponseEntity.ok(okTgReply(
                                chatId,
                                "Hola " + firstName + "! El teu compte s'ha vinculat correctament. ✅\n\nJa pots gestionar els teus serveis des d'aquest xat."
                        ));
                    }
                }
                return ResponseEntity.ok(okTgReply(
                        chatId,
                        "Hola " + firstName + "! 👋\n\nUtilitza el codi d'enllaç que t'ha proporcionat l'administrador per vincular el teu compte."
                ));
            }

            // Route message to agent by finding linked tenant
            var chatLinkOpt = chatLinkRepository.findByTelegramChatId(chatId);
            if (chatLinkOpt.isPresent() && chatLinkOpt.get().getIsActive()) {
                var link = chatLinkOpt.get();
                var tenantId = link.getTenantId();

                // Resposta pendent a una ressenya de Google (Mòdul 54) — té prioritat
                if (!text.startsWith("/") && reviewReplyService.hasPending(chatId)) {
                    var reply = reviewReplyService.submitReply(tenantId, chatId, text);
                    if (reply != null) return ResponseEntity.ok(okTgReply(chatId, reply));
                }

                // Resposta pendent a un comentari de Facebook (Mòdul 55, feature 1)
                if (!text.startsWith("/") && commentReplyService.hasPending(chatId)) {
                    var reply = commentReplyService.submitReply(tenantId, chatId, text);
                    if (reply != null) return ResponseEntity.ok(okTgReply(chatId, reply));
                }

                // Resposta manual pendent a un DM d'IG/Messenger (Mòdul 56 F2)
                if (!text.startsWith("/") && socialDmService.hasPending(chatId)) {
                    var reply = socialDmService.submitManualReply(chatId, text);
                    if (reply != null) return ResponseEntity.ok(okTgReply(chatId, reply));
                }

                // Detecció de creixement d'equip (upsell F5 si 2a+ persona)
                if (fromUserId != null) {
                    teamGrowthService.recordAndCheck(tenantId, fromUserId, firstName, chatId);
                }

                // Comanda d'absència: /absencia [data]
                if (text.toLowerCase().startsWith("/absencia")) {
                    var reply = absenceRescheduleService.handleAbsenceCommand(
                            tenantId, text, chatId, fromUserId);
                    return ResponseEntity.ok(okTgReply(chatId, reply));
                }

                // Comanda de festiu: /festiu [data]
                if (text.toLowerCase().startsWith("/festiu")) {
                    var reply = absenceRescheduleService.handleHolidayCommand(tenantId, text);
                    return ResponseEntity.ok(okTgReply(chatId, reply));
                }

                // /ajuda — llista de comandes disponibles
                if (text.equalsIgnoreCase("/ajuda") || text.equalsIgnoreCase("/help")
                        || text.equalsIgnoreCase("/start help")) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            tenantCommandService.handleAjuda(tenantId)));
                }

                // /mode [auto|manual|hybrid] — canviar mode agent (F1)
                if (text.toLowerCase().startsWith("/mode")) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            tenantCommandService.handleMode(tenantId, text)));
                }

                // /stats — resum d'activitat (F1)
                if (text.equalsIgnoreCase("/stats") || text.equalsIgnoreCase("/resum")) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            tenantCommandService.handleStats(tenantId)));
                }

                // /pendents — documents sense resposta (F3)
                if (text.equalsIgnoreCase("/pendents")) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            tenantCommandService.handlePendents(tenantId)));
                }

                // /cancel [hora] — cancel·lar cita (F2)
                if (text.toLowerCase().startsWith("/cancel")) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            tenantCommandService.handleCancel(tenantId, text)));
                }

                // /reviews — darreres ressenyes Google (F4)
                if (text.equalsIgnoreCase("/reviews") || text.equalsIgnoreCase("/ressenyes")) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            tenantCommandService.handleReviews(tenantId)));
                }

                // Comanda d'agenda: /agenda [avui|demà|setmana|YYYY-MM-DD]
                if (text.toLowerCase().startsWith("/agenda")) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            tenantAgendaQueryService.handleCommand(tenantId, text)));
                }

                // Comanda de pressupost: /pressupost email nom [notes]
                if (text.toLowerCase().startsWith("/pressupost")) {
                    var reply = budgetWorkflowService.handleCommand(tenantId, chatId, text);
                    return ResponseEntity.ok(okTgReply(chatId, reply));
                }

                // Aprovació/rebuig d'un pressupost pendent
                if (budgetWorkflowService.hasPendingApproval(tenantId, chatId)) {
                    String normalized = text.trim().toLowerCase()
                        .replace("í", "i").replace("é", "e");
                    if (normalized.matches("si|sí|yes|✅|👍|confirmar|enviar")) {
                        var reply = budgetWorkflowService.approve(tenantId, chatId);
                        if (reply != null) return ResponseEntity.ok(okTgReply(chatId, reply));
                    } else if (normalized.matches("no|cancel·lar|cancelar|❌|👎")) {
                        var reply = budgetWorkflowService.reject(tenantId, chatId);
                        if (reply != null) return ResponseEntity.ok(okTgReply(chatId, reply));
                    }
                }

                // Pas d'un flux de publicació social actiu (prioritat: continua el flux existent)
                if (socialOrchestrator.hasDraft(chatId)) {
                    socialOrchestrator.handleStep(chatId, text, photoFileId);
                    return ResponseEntity.ok("ok");
                }

                // Foto sense flux actiu → possible resposta al brief del Content Planner (Mòdul 58)
                if (photoFileId != null
                        && contentPlannerFlow.handleIncomingPhoto(chatId, tenantId, photoFileId)) {
                    return ResponseEntity.ok("ok");
                }

                // Comanda /publica o paraules clau de publicació social
                boolean isSocialIntent = text.toLowerCase().startsWith("/publica")
                    || (!text.startsWith("/") && SOCIAL_TRIGGER.matcher(text).find());
                if (isSocialIntent) {
                    boolean activated = nexeServiceConfigService
                        .get(tenantId, "SOCIAL_PUBLISHER").isPresent();
                    if (!activated) {
                        return ResponseEntity.ok(okTgReply(chatId,
                            "ℹ️ El servei de publicació a xarxes socials no està activat per al teu compte.\n"
                            + "Contacta l'administrador per activar-lo."));
                    }
                    socialOrchestrator.startFlow(tenantId, chatId);
                    return ResponseEntity.ok("ok");
                }

                // Try to route to an agent
                boolean handled = false;
                for (var agent : agentRegistry.getAllAgents()) {
                    var response = agent.handleMessage(tenantId, text, chatId);
                    if (response != null && !response.isBlank()) {
                        return ResponseEntity.ok(okTgReply(chatId, response));
                    }
                    handled = true;
                }
                if (!handled) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            "No s'ha pogut processar el missatge. Prova de nou més tard."));
                }
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.ok("ok");
        }
    }

    // Endpoint per-tenant: cada client té el seu bot amb la seva URL
    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<String> handleCustomerWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> payload) {
        String expectedSecret = systemConfigService.get("TELEGRAM_WEBHOOK_SECRET");
        if (expectedSecret != null && !expectedSecret.isBlank()) {
            if (!expectedSecret.equals(secretToken)) {
                log.warn("Telegram webhook (tenant {}): secret_token invàlid o absent", tenantId);
                return ResponseEntity.status(401).body("Unauthorized");
            }
        }
        try {
            var message = extractMessage(payload);
            if (message == null) return ResponseEntity.ok("ok");

            var chatId = message.get("chat") instanceof Map<?, ?> chat
                    ? ((Number) chat.get("id")).longValue() : null;
            var text = message.get("text") instanceof String t ? t.trim() : "";

            if (chatId == null || text.isBlank()) return ResponseEntity.ok("ok");

            log.info("Missatge TG client rebut per tenant={} de chat={}: {}", tenantId, chatId,
                    text.substring(0, Math.min(text.length(), 50)));

            handleCustomerAsync(tenantId, chatId, text);
        } catch (Exception e) {
            log.error("Error processing customer Telegram webhook: {}", e.getMessage());
        }
        return ResponseEntity.ok("ok");
    }

    @Async
    public void handleCustomerAsync(UUID tenantId, Long chatId, String text) {
        conversationalAgentService.handleIncoming(tenantId, chatId.toString(), ConversationChannel.TELEGRAM, text);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMessage(Map<String, Object> payload) {
        if (payload.get("message") instanceof Map<?, ?> msg) {
            return (Map<String, Object>) msg;
        }
        if (payload.get("edited_message") instanceof Map<?, ?> msg) {
            return (Map<String, Object>) msg;
        }
        return null;
    }

    private String okTgReply(Long chatId, String text) {
        return "{\"method\":\"sendMessage\",\"chat_id\":" + chatId + ",\"text\":" + JSONescape(text) + "}";
    }

    private String JSONescape(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
