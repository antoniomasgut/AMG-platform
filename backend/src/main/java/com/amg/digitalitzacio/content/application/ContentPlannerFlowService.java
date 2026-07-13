package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.content.domain.*;
import com.amg.digitalitzacio.social.application.SocialContentGeneratorService;
import com.amg.digitalitzacio.social.application.TelegramMediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sub-flux del Content Planner sobre Telegram (Spec 58 §7). NO reutilitza el flux de passos
 * del Social Publisher: la foto arriba sense draft actiu com a resposta al brief de la setmana.
 * Rep la foto → genera el caption (pilar + idioma) → demana confirmació ✅/✍️ → publica.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ContentPlannerFlowService {

    private final ContentPlanItemRepository itemRepository;
    private final ContentPlanRepository planRepository;
    private final TenantRepository tenantRepository;
    private final TelegramMediaUploadService mediaUploadService;
    private final SocialContentGeneratorService contentGenerator;
    private final ContentPlanPublishService publishService;
    private final TelegramBotClient telegramBotClient;

    /**
     * Intenta tractar una foto entrant com a resposta al brief de la setmana.
     * @return true si l'ha gestionada (hi havia un item pendent de foto); false si no.
     */
    public boolean handleIncomingPhoto(Long chatId, UUID tenantId, String photoFileId) {
        Optional<ContentPlanItem> pending = findAwaitingPhoto(tenantId);
        if (pending.isEmpty()) return false;
        ContentPlanItem item = pending.get();

        telegramBotClient.sendMessage(chatId, "⏳ Rebent la foto…");
        String mediaUrl;
        try {
            mediaUrl = mediaUploadService.downloadAndUpload(photoFileId, tenantId);
        } catch (Exception e) {
            log.warn("Content planner: error pujant foto tenant {}: {}", tenantId, e.getMessage());
            telegramBotClient.sendMessage(chatId, "⚠️ No s'ha pogut processar la foto. Torna-la a enviar, si us plau.");
            return true;
        }

        item.setMediaUrl(mediaUrl);
        item.setStatus(ContentItemStatus.PHOTO_RECEIVED);
        item.setCaption(generateCaption(item, tenantId));
        item.setStatus(ContentItemStatus.AWAITING_APPROVAL);
        itemRepository.save(item);
        sendApprovalMessage(chatId, item);
        return true;
    }

    /** El tenant confirma (✅): publica l'item a les seves xarxes. */
    public void approve(Long chatId, UUID tenantId, UUID itemId) {
        ContentPlanItem item = ownedAwaiting(tenantId, itemId);
        if (item == null) {
            telegramBotClient.sendMessage(chatId, "Aquest post ja no està pendent de confirmació.");
            return;
        }
        telegramBotClient.sendMessage(chatId, "⏳ Publicant…");
        ContentPlanItem result = publishService.publishItem(item);
        if (result.getStatus() == ContentItemStatus.PUBLISHED) {
            telegramBotClient.sendMessage(chatId, "🎉 Publicat! La setmana que ve, nou tema 😉");
        } else if (result.getStatus() == ContentItemStatus.SKIPPED) {
            // Cap xarxa connectada: el reintent no ajudaria; cal connectar-les primer.
            telegramBotClient.sendMessage(chatId, "⚠️ " + result.getError());
        } else {
            telegramBotClient.sendMessage(chatId,
                    "⚠️ No s'ha pogut publicar ara mateix. Ho reintentarem automàticament.");
        }
    }

    /** El tenant demana refer el text (✍️): regenera el caption i torna a demanar confirmació. */
    public void rewrite(Long chatId, UUID tenantId, UUID itemId) {
        ContentPlanItem item = ownedAwaiting(tenantId, itemId);
        if (item == null) return;
        item.setCaption(generateCaption(item, tenantId));
        itemRepository.save(item);
        sendApprovalMessage(chatId, item);
    }

    // ─────────────────────── interns ───────────────────────

    private Optional<ContentPlanItem> findAwaitingPhoto(UUID tenantId) {
        return itemRepository.findByTenantIdAndStatus(tenantId, ContentItemStatus.PHOTO_REQUESTED).stream()
                .filter(i -> i.getMediaUrl() == null)
                .findFirst();
    }

    private ContentPlanItem ownedAwaiting(UUID tenantId, UUID itemId) {
        return itemRepository.findById(itemId)
                .filter(i -> i.getTenantId().equals(tenantId))
                .filter(i -> i.getStatus() == ContentItemStatus.AWAITING_APPROVAL)
                .orElse(null);
    }

    private void sendApprovalMessage(Long chatId, ContentPlanItem item) {
        String text = "📝 <b>Aquí tens el post preparat:</b>\n\n" + item.getCaption()
                + "\n\nEl publicam?";
        telegramBotClient.sendMessageWithButtons(chatId, text, List.of(
                Map.of("text", "✅ Publicar", "callback_data", "cpok:" + item.getId()),
                Map.of("text", "✍️ Canvia-ho", "callback_data", "cpwr:" + item.getId())));
    }

    private String generateCaption(ContentPlanItem item, UUID tenantId) {
        String lang = resolveLanguage(item);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        String ctx = tenant != null
                ? tenant.getName() + (tenant.getSector() != null ? " (" + tenant.getSector().name() + ")" : "")
                : "";
        String brief = (item.getBriefText() != null ? item.getBriefText() : "")
                + " Escriu el post en " + languageName(lang) + ".";
        try {
            String caption = contentGenerator.generateCaption("INSTAGRAM", ctx, brief);
            if (caption != null && !caption.isBlank()) return caption;
        } catch (Exception e) {
            log.warn("Content planner: error generant caption item {}: {}", item.getId(), e.getMessage());
        }
        return item.getBriefText() != null ? item.getBriefText() : "";
    }

    private String resolveLanguage(ContentPlanItem item) {
        if (item.getContentLanguage() != null && !item.getContentLanguage().isBlank()) {
            return item.getContentLanguage();
        }
        return planRepository.findById(item.getPlanId())
                .map(ContentPlan::getContentLanguage)
                .filter(l -> l != null && !l.isBlank())
                .orElse("ca");
    }

    private String languageName(String code) {
        return switch (code) {
            case "es" -> "castellà";
            case "en" -> "anglès";
            case "de" -> "alemany";
            default -> "català";
        };
    }
}
