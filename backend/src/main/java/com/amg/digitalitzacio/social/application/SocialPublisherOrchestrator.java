package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfig;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Màquina d'estats Telegram per publicació social (Spec 52).
 *
 * Clau Redis: social:draft:{chatId}  TTL: 30 min
 * Step: AWAIT_NETWORKS → AWAIT_TYPE → AWAIT_MEDIA → AWAIT_CAPTION → AWAIT_CONFIRM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialPublisherOrchestrator {

    private static final String KEY_PREFIX = "social:draft:";
    private static final long TTL_MINUTES  = 30;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final TelegramBotClient telegramBotClient;
    private final TenantRepository tenantRepository;
    private final SocialMetaConfigRepository metaConfigRepo;
    private final SocialPostRepository postRepository;
    private final GoogleModuleConfigRepository googleConfigRepo;
    private final VaultEncryption vaultEncryption;
    private final SocialContentGeneratorService contentGenerator;
    private final InstagramPublisherService instagramPublisher;
    private final FacebookPublisherService facebookPublisher;
    private final GoogleBusinessPublisherService googleBusinessPublisher;
    private final TelegramMediaUploadService telegramMediaUploadService;

    /** Retorna true si hi ha un draft actiu per a aquest chatId */
    public boolean hasDraft(Long chatId) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + chatId));
    }

    /** Inicia el flux de publicació per a un tenant */
    @Async
    public void startFlow(UUID tenantId, Long chatId) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        String businessName = tenant != null ? tenant.getName() : "el teu negoci";

        var draft = new HashMap<String, String>();
        draft.put("tenantId",  tenantId.toString());
        draft.put("step",      "AWAIT_NETWORKS");
        draft.put("business",  businessName);
        saveDraft(chatId, draft);

        var networks = buildAvailableNetworks(tenantId);
        telegramBotClient.sendMessage(chatId,
            "📲 <b>Nou post — " + businessName + "</b>\n\n"
            + "On vols publicar?\n"
            + networks
            + "\nEscriu les lletres separades per espai, p.ex.: <code>I F</code>");
    }

    /** Processa cada pas del flux. photoFileId és no-null quan l'usuari envia una foto. */
    @Async
    public void handleStep(Long chatId, String text, String photoFileId) {
        var draft = loadDraft(chatId);
        if (draft == null) return;

        String step = draft.get("step");

        switch (step) {
            case "AWAIT_NETWORKS" -> handleNetworks(chatId, text, draft);
            case "AWAIT_TYPE"     -> handleType(chatId, text, draft);
            case "AWAIT_MEDIA"    -> handleMedia(chatId, text, photoFileId, draft);
            case "AWAIT_CAPTION"  -> handleCaption(chatId, text, draft);
            case "AWAIT_CONFIRM"  -> handleConfirm(chatId, text, draft);
            default -> {
                redis.delete(KEY_PREFIX + chatId);
                telegramBotClient.sendMessage(chatId, "❌ Flux cancel·lat. Torna a escriure <code>/publica</code>.");
            }
        }
    }

    // ─── Steps ───────────────────────────────────────────────────────────────

    private void handleNetworks(Long chatId, String text, Map<String, String> draft) {
        String upper = text.toUpperCase().replace(",", " ").trim();
        boolean ig = upper.contains("I");
        boolean fb = upper.contains("F");
        boolean gb = upper.contains("G");

        if (!ig && !fb && !gb) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Indica almenys una xarxa: <b>I</b> (Instagram), <b>F</b> (Facebook), <b>G</b> (Google Business).");
            return;
        }

        draft.put("ig", ig ? "1" : "0");
        draft.put("fb", fb ? "1" : "0");
        draft.put("gb", gb ? "1" : "0");
        draft.put("step", "AWAIT_TYPE");
        saveDraft(chatId, draft);

        telegramBotClient.sendMessage(chatId,
            "Quin tipus de contingut?\n"
            + (ig ? "📸 <code>FOTO</code> — Foto amb caption\n" : "")
            + (fb ? "📝 <code>TEXT</code> — Missatge de text\n   📸 <code>FOTO</code> — Foto amb caption\n" : "")
            + (gb ? "🗺 <code>NOTICIES</code> — Notícia\n   🎉 <code>OFERTA</code> — Oferta\n" : "")
            + "\nEscriu el tipus (p.ex. <code>FOTO</code>):");
    }

    private void handleType(Long chatId, String text, Map<String, String> draft) {
        String t = text.toUpperCase().trim();
        String postType = switch (t) {
            case "FOTO"     -> "PHOTO";
            case "TEXT"     -> "TEXT";
            case "NOTICIES" -> "WHATS_NEW";
            case "OFERTA"   -> "OFFER";
            case "EVENT"    -> "EVENT";
            default         -> null;
        };

        if (postType == null) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Opció no reconeguda. Escriu: <code>FOTO</code>, <code>TEXT</code>, <code>NOTICIES</code>, <code>OFERTA</code> o <code>EVENT</code>.");
            return;
        }

        draft.put("postType", postType);
        draft.put("step", "AWAIT_CAPTION");
        saveDraft(chatId, draft);

        telegramBotClient.sendMessage(chatId,
            "✍️ Escriu el text del post o <code>IA</code> per generar-lo automàticament:");
    }

    private void handleMedia(Long chatId, String text, String photoFileId, Map<String, String> draft) {
        // Cas 1: l'usuari ha enviat una foto directament per Telegram
        if (photoFileId != null) {
            telegramBotClient.sendMessage(chatId, "⏳ Pujant la foto…");
            try {
                UUID tenantId = UUID.fromString(draft.get("tenantId"));
                String url = telegramMediaUploadService.downloadAndUpload(photoFileId, tenantId);
                draft.put("mediaUrl", url);
            } catch (Exception e) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ No s'ha pogut pujar la foto: " + e.getMessage()
                    + "\nEnvia una URL o escriu <code>SENSE_FOTO</code>.");
                return;
            }
        } else {
            // Cas 2: l'usuari escriu una URL o SENSE_FOTO
            String url = text.trim();
            if (!url.equalsIgnoreCase("SENSE_FOTO")) {
                if (!url.startsWith("http")) {
                    telegramBotClient.sendMessage(chatId,
                        "⚠️ Envia la foto directament o una URL vàlida (http/https), o escriu <code>SENSE_FOTO</code>.");
                    return;
                }
                draft.put("mediaUrl", url);
            }
        }

        draft.put("step", "AWAIT_CONFIRM");
        saveDraft(chatId, draft);
        sendPreview(chatId, draft, draft.get("caption"));
    }

    private void sendPreview(Long chatId, Map<String, String> draft, String caption) {
        String preview = "📋 <b>Resum del post:</b>\n"
            + "Xarxes: " + buildNetworkList(draft) + "\n"
            + "Tipus: " + draft.get("postType") + "\n"
            + (draft.containsKey("mediaUrl") ? "🖼 Imatge: " + draft.get("mediaUrl") + "\n" : "")
            + "\n" + (caption != null ? caption : "")
            + "\n\n✅ Escriu <code>SI</code> per publicar o <code>NO</code> per cancel·lar.";
        telegramBotClient.sendMessage(chatId, preview);
    }

    private void handleCaption(Long chatId, String text, Map<String, String> draft) {
        String caption;
        if (text.trim().equalsIgnoreCase("IA")) {
            String business = draft.getOrDefault("business", "el negoci");
            boolean ig = "1".equals(draft.get("ig"));
            String network = ig ? "INSTAGRAM" : ("1".equals(draft.get("fb")) ? "FACEBOOK" : "GOOGLE_BUSINESS");
            caption = contentGenerator.generateCaption(network, business, "Post professional per a " + business);
            telegramBotClient.sendMessage(chatId, "🤖 Caption generat per IA:\n\n" + caption);
        } else {
            caption = text.trim();
        }

        draft.put("caption", caption);

        boolean needsMedia = "PHOTO".equals(draft.get("postType"));
        if (needsMedia) {
            draft.put("step", "AWAIT_MEDIA");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "📸 Envia la foto directament aquí, o una URL pública:\n"
                + "Exemple: <code>https://cdn.amgdl.com/fotos/foto.jpg</code>\n\n"
                + "O escriu <code>SENSE_FOTO</code> per publicar sense imatge.");
        } else {
            draft.put("step", "AWAIT_CONFIRM");
            saveDraft(chatId, draft);
            sendPreview(chatId, draft, caption);
        }
    }

    private void handleConfirm(Long chatId, String text, Map<String, String> draft) {
        String normalized = text.trim().toLowerCase().replace("í", "i").replace("é", "e");
        redis.delete(KEY_PREFIX + chatId);

        if (!normalized.matches("si|yes|✅|👍|publicar|confirmar")) {
            telegramBotClient.sendMessage(chatId, "❌ Publicació cancel·lada.");
            return;
        }

        UUID tenantId = UUID.fromString(draft.get("tenantId"));
        telegramBotClient.sendMessage(chatId, "⏳ Publicant a les xarxes seleccionades…");

        publishAsync(tenantId, chatId, draft);
    }

    @Async
    public void publishAsync(UUID tenantId, Long chatId, Map<String, String> draft) {
        boolean ig = "1".equals(draft.get("ig"));
        boolean fb = "1".equals(draft.get("fb"));
        boolean gb = "1".equals(draft.get("gb"));
        String postType  = draft.get("postType");
        String caption   = draft.get("caption");
        String mediaUrl  = draft.get("mediaUrl");

        var results = new StringBuilder("📊 <b>Resultats:</b>\n");
        var metaConfigOpt = metaConfigRepo.findByTenantId(tenantId);

        if (ig && metaConfigOpt.isPresent()) {
            try {
                var mc = metaConfigOpt.get();
                String token = vaultEncryption.decrypt(mc.getPageAccessTokenEncrypted());
                String extId = instagramPublisher.publishFeedPhoto(mc.getInstagramAccountId(), token,
                    mediaUrl != null ? mediaUrl : "", caption != null ? caption : "");
                savePost(tenantId, "INSTAGRAM", postType, caption, mediaUrl, extId, null, "PUBLISHED");
                results.append("✅ Instagram publicat\n");
            } catch (Exception e) {
                savePost(tenantId, "INSTAGRAM", postType, caption, mediaUrl, null, e.getMessage(), "FAILED");
                results.append("❌ Instagram: ").append(e.getMessage()).append("\n");
            }
        }

        if (fb && metaConfigOpt.isPresent()) {
            try {
                var mc = metaConfigOpt.get();
                String token = vaultEncryption.decrypt(mc.getPageAccessTokenEncrypted());
                String extId;
                if ("PHOTO".equals(postType) && mediaUrl != null) {
                    extId = facebookPublisher.publishPhoto(mc.getFacebookPageId(), token, mediaUrl, caption);
                } else {
                    extId = facebookPublisher.publishText(mc.getFacebookPageId(), token, caption != null ? caption : "");
                }
                savePost(tenantId, "FACEBOOK", postType, caption, mediaUrl, extId, null, "PUBLISHED");
                results.append("✅ Facebook publicat\n");
            } catch (Exception e) {
                savePost(tenantId, "FACEBOOK", postType, caption, mediaUrl, null, e.getMessage(), "FAILED");
                results.append("❌ Facebook: ").append(e.getMessage()).append("\n");
            }
        }

        if (gb) {
            try {
                var gConfig = googleConfigRepo.findById(tenantId).orElse(null);
                String locationName = gConfig != null ? gConfig.getBusinessLocationId() : null;
                if (locationName == null || locationName.isBlank()) {
                    results.append("⚠️ Google Business: ubicació no configurada\n");
                } else {
                    String extId = switch (postType) {
                        case "OFFER"    -> googleBusinessPublisher.publishOffer(tenantId, locationName, caption, mediaUrl);
                        case "EVENT"    -> googleBusinessPublisher.publishEvent(tenantId, locationName, caption, caption, mediaUrl);
                        default         -> googleBusinessPublisher.publishWhatsNew(tenantId, locationName, caption, mediaUrl);
                    };
                    savePost(tenantId, "GOOGLE_BUSINESS", postType, caption, mediaUrl, extId, null, "PUBLISHED");
                    results.append("✅ Google Business publicat\n");
                }
            } catch (Exception e) {
                savePost(tenantId, "GOOGLE_BUSINESS", postType, caption, mediaUrl, null, e.getMessage(), "FAILED");
                results.append("❌ Google Business: ").append(e.getMessage()).append("\n");
            }
        }

        if (chatId != null) {
            telegramBotClient.sendMessage(chatId, results.toString());
        } else {
            log.info("Social publish results (scheduled): {}", results);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildAvailableNetworks(UUID tenantId) {
        var sb = new StringBuilder();
        var mc = metaConfigRepo.findByTenantId(tenantId);
        boolean hasIg = mc.isPresent() && mc.get().getInstagramAccountId() != null;
        boolean hasFb = mc.isPresent() && mc.get().getFacebookPageId() != null;
        var gc = googleConfigRepo.findById(tenantId);
        boolean hasGb = gc.isPresent() && gc.get().isBusinessEnabled();

        if (hasIg) sb.append("📷 <b>I</b> — Instagram\n");
        if (hasFb) sb.append("👥 <b>F</b> — Facebook Page\n");
        if (hasGb) sb.append("🗺 <b>G</b> — Google Business\n");

        if (sb.isEmpty()) sb.append("⚠️ Cap xarxa configurada. Contacta l'administrador.\n");
        return sb.toString();
    }

    private String buildNetworkList(Map<String, String> draft) {
        var list = new StringBuilder();
        if ("1".equals(draft.get("ig"))) list.append("Instagram ");
        if ("1".equals(draft.get("fb"))) list.append("Facebook ");
        if ("1".equals(draft.get("gb"))) list.append("Google Business ");
        return list.toString().trim();
    }

    private void savePost(UUID tenantId, String network, String postType, String caption,
                          String mediaUrl, String extId, String errorMsg, String status) {
        try {
            var post = SocialPost.builder()
                .tenantId(tenantId)
                .network(network)
                .postType(postType)
                .caption(caption)
                .mediaUrl(mediaUrl)
                .externalPostId(extId)
                .status(status)
                .publishedAt("PUBLISHED".equals(status) ? Instant.now() : null)
                .errorMessage(errorMsg)
                .build();
            postRepository.save(post);
        } catch (Exception e) {
            log.warn("Error desant SocialPost per tenant {}: {}", tenantId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadDraft(Long chatId) {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + chatId);
            if (json == null) return null;
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Error carregant draft social per chat {}: {}", chatId, e.getMessage());
            return null;
        }
    }

    private void saveDraft(Long chatId, Map<String, String> draft) {
        try {
            redis.opsForValue().set(KEY_PREFIX + chatId,
                objectMapper.writeValueAsString(draft), TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Error desant draft social per chat {}: {}", chatId, e.getMessage());
        }
    }
}
