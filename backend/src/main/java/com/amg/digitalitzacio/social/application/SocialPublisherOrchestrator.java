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
    private final LinkedInPublisherService linkedInPublisher;
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

    /** Processa cada pas del flux. photoFileId/videoFileId són no-null quan l'usuari envia mèdia. */
    @Async
    public void handleStep(Long chatId, String text, String photoFileId, String videoFileId) {
        var draft = loadDraft(chatId);
        if (draft == null) return;

        String step = draft.get("step");

        switch (step) {
            case "AWAIT_NETWORKS" -> handleNetworks(chatId, text, draft);
            case "AWAIT_TYPE"     -> handleType(chatId, text, draft);
            case "AWAIT_MEDIA"    -> handleMedia(chatId, text, photoFileId, videoFileId, draft);
            case "AWAIT_CAPTION"  -> handleCaption(chatId, text, draft);
            case "AWAIT_SCHEDULE" -> handleSchedule(chatId, text, draft);
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
        boolean li = upper.contains("L");

        if (!ig && !fb && !gb && !li) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Indica almenys una xarxa: <b>I</b> (Instagram), <b>F</b> (Facebook), <b>G</b> (Google Business).");
            return;
        }

        // LinkedIn només per al tenant propietari amb connexió activa
        if (li && !isLinkedInAvailable(UUID.fromString(draft.get("tenantId")))) {
            li = false;
            if (!ig && !fb && !gb) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ LinkedIn no està disponible per a aquest compte. "
                    + "Tria una altra xarxa: <b>I</b> (Instagram), <b>F</b> (Facebook) o <b>G</b> (Google Business).");
                return;
            }
        }

        draft.put("ig", ig ? "1" : "0");
        draft.put("fb", fb ? "1" : "0");
        draft.put("gb", gb ? "1" : "0");
        draft.put("li", li ? "1" : "0");
        draft.put("step", "AWAIT_TYPE");
        saveDraft(chatId, draft);

        telegramBotClient.sendMessage(chatId,
            "Quin tipus de contingut?\n"
            + (ig ? "📸 <code>FOTO</code> — Foto amb caption\n   🎬 <code>VIDEO</code> — Reel (vídeo al feed)\n   ⭕ <code>STORY</code> — Story (foto o vídeo, 24h)\n" : "")
            + (fb ? "📝 <code>TEXT</code> — Missatge de text\n   📸 <code>FOTO</code> — Foto amb caption\n"
                    + (!ig ? "   🎬 <code>VIDEO</code> — Vídeo a la pàgina\n   ⭕ <code>STORY</code> — Story de foto (24h)\n" : "") : "")
            + (gb ? "🗺 <code>NOTICIES</code> — Notícia\n   🎉 <code>OFERTA</code> — Oferta\n" : "")
            + (li ? "💼 <code>TEXT</code> — Publicació a LinkedIn\n" : "")
            + "\nEscriu el tipus (p.ex. <code>FOTO</code>):");
    }

    /** LinkedIn només disponible per al tenant propietari amb connexió activa (Mòdul 56 F4) */
    private boolean isLinkedInAvailable(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        boolean owner = tenant != null && Boolean.TRUE.equals(tenant.getIsOwner());
        return owner && linkedInPublisher.isConnected(tenantId);
    }

    private void handleType(Long chatId, String text, Map<String, String> draft) {
        String t = text.toUpperCase().trim();
        String postType = switch (t) {
            case "FOTO"            -> "PHOTO";
            case "TEXT"            -> "TEXT";
            case "VIDEO", "REEL"   -> "REEL";
            case "STORY", "STORIES"-> "STORY";
            case "NOTICIES"        -> "WHATS_NEW";
            case "OFERTA"          -> "OFFER";
            case "EVENT"           -> "EVENT";
            default                -> null;
        };

        if (postType == null) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Opció no reconeguda. Escriu: <code>FOTO</code>, <code>TEXT</code>, <code>VIDEO</code>, <code>STORY</code>, <code>NOTICIES</code>, <code>OFERTA</code> o <code>EVENT</code>.");
            return;
        }

        // Vídeo i Story només van a IG/FB
        if ("REEL".equals(postType) || "STORY".equals(postType)) {
            boolean hasIgFb = "1".equals(draft.get("ig")) || "1".equals(draft.get("fb"));
            if (!hasIgFb) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ Els vídeos i stories només es publiquen a Instagram/Facebook, "
                    + "i no n'has seleccionat cap. Tria un altre tipus de contingut.");
                return;
            }
            if ("1".equals(draft.get("gb")) || "1".equals(draft.get("li"))) {
                telegramBotClient.sendMessage(chatId,
                    "ℹ️ Els vídeos i stories només es publiquen a Instagram/Facebook. "
                    + "Google Business i LinkedIn s'ometran per a aquest post.");
            }
        }

        draft.put("postType", postType);

        // Les stories no porten text: salta directament al mèdia
        if ("STORY".equals(postType)) {
            draft.put("step", "AWAIT_MEDIA");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "⭕ Envia la foto o el vídeo de la story directament aquí (o una URL pública):");
            return;
        }

        draft.put("step", "AWAIT_CAPTION");
        saveDraft(chatId, draft);

        telegramBotClient.sendMessage(chatId,
            "✍️ Escriu el text del post o <code>IA</code> per generar-lo automàticament:");
    }

    private void handleMedia(Long chatId, String text, String photoFileId, String videoFileId,
                             Map<String, String> draft) {
        String postType = draft.get("postType");
        boolean wantsVideo = "REEL".equals(postType);

        // Validació: un Reel necessita vídeo; una FOTO necessita foto
        if (wantsVideo && photoFileId != null && videoFileId == null) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Has triat <b>VIDEO</b> però has enviat una foto. Envia un vídeo (MP4, màx 20 MB).");
            return;
        }
        if ("PHOTO".equals(postType) && videoFileId != null) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Has triat <b>FOTO</b> però has enviat un vídeo. Envia una foto, o cancel·la i tria <code>VIDEO</code>.");
            return;
        }

        String fileId = videoFileId != null ? videoFileId : photoFileId;

        // Cas 1: l'usuari ha enviat el mèdia directament per Telegram
        if (fileId != null) {
            telegramBotClient.sendMessage(chatId, videoFileId != null ? "⏳ Pujant el vídeo…" : "⏳ Pujant la foto…");
            try {
                UUID tenantId = UUID.fromString(draft.get("tenantId"));
                String url = telegramMediaUploadService.downloadAndUpload(fileId, tenantId);
                draft.put("mediaUrl", url);
                draft.put("mediaKind", videoFileId != null ? "video" : "image");
            } catch (Exception e) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ No s'ha pogut pujar el mèdia: " + e.getMessage()
                    + "\nEnvia una URL o escriu <code>SENSE_FOTO</code>.");
                return;
            }
        } else {
            // Cas 2: l'usuari escriu una URL o SENSE_FOTO
            String url = text.trim();
            if (!url.equalsIgnoreCase("SENSE_FOTO")) {
                if (!url.startsWith("http")) {
                    telegramBotClient.sendMessage(chatId,
                        "⚠️ Envia el mèdia directament o una URL vàlida (http/https), o escriu <code>SENSE_FOTO</code>.");
                    return;
                }
                draft.put("mediaUrl", url);
                String lower = url.toLowerCase();
                draft.put("mediaKind",
                    lower.contains(".mp4") || lower.contains(".mov") || wantsVideo ? "video" : "image");
            } else if (wantsVideo || "STORY".equals(postType)) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ Un " + ("STORY".equals(postType) ? "story" : "Reel") + " necessita mèdia obligatòriament.");
                return;
            }
        }

        askWhenToPublish(chatId, draft);
    }

    private static final java.time.ZoneId ZONE_ES = java.time.ZoneId.of("Europe/Madrid");
    private static final java.util.regex.Pattern PAT_TIME =
        java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static final java.util.regex.Pattern PAT_DATE_FULL =
        java.util.regex.Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final java.util.regex.Pattern PAT_DATE_SHORT =
        java.util.regex.Pattern.compile("(\\d{1,2})/(\\d{1,2})");

    private void askWhenToPublish(Long chatId, Map<String, String> draft) {
        draft.put("step", "AWAIT_SCHEDULE");
        saveDraft(chatId, draft);
        telegramBotClient.sendMessage(chatId,
            "⏰ Quan vols publicar?\n\n"
            + "<code>ARA</code> — publicar immediatament\n"
            + "<code>avui a les 22:00</code> — avui a l'hora indicada\n"
            + "<code>demà a les 09:30</code> — demà\n"
            + "<code>15/07 18:00</code> — data sense any (any actual)\n"
            + "<code>15/07/2026 18:00</code> — data amb any completa");
    }

    private void handleSchedule(Long chatId, String text, Map<String, String> draft) {
        String lower = text.trim().toLowerCase();

        if (lower.equals("ara") || lower.equals("now")) {
            draft.put("step", "AWAIT_CONFIRM");
            saveDraft(chatId, draft);
            sendPreview(chatId, draft, draft.get("caption"));
            return;
        }

        Instant scheduledAt = parseScheduleInput(lower);
        if (scheduledAt == null) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Format no reconegut. Exemples:\n"
                + "<code>ARA</code>\n"
                + "<code>avui a les 22:00</code>\n"
                + "<code>demà a les 09:30</code>\n"
                + "<code>15/07 18:00</code>\n"
                + "<code>15/07/2026 a les 07:00</code>");
            return;
        }

        if (scheduledAt.isBefore(Instant.now())) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ La data ha de ser futura. Torna a intentar-ho o escriu <code>ARA</code>.");
            return;
        }

        redis.delete(KEY_PREFIX + chatId);
        UUID tenantId = UUID.fromString(draft.get("tenantId"));
        for (String net : resolveNetworks(draft)) {
            postRepository.save(SocialPost.builder()
                .tenantId(tenantId)
                .network(net)
                .postType(draft.get("postType"))
                .caption(draft.get("caption"))
                .mediaUrl(draft.get("mediaUrl"))
                .status("SCHEDULED")
                .scheduledAt(scheduledAt)
                .build());
        }

        String formatted = java.time.format.DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZONE_ES)
            .format(scheduledAt);
        telegramBotClient.sendMessage(chatId,
            "✅ Post programat per al <b>" + formatted + "</b>.\n"
            + "Pots veure'l i cancel·lar-lo des del portal.");
    }

    /** Parseja expressions de data/hora en català i format numèric. */
    private Instant parseScheduleInput(String lower) {
        // Extreu l'hora — obligatòria en tots els formats
        var timeMatcher = PAT_TIME.matcher(lower);
        if (!timeMatcher.find()) return null;
        int hour   = Integer.parseInt(timeMatcher.group(1));
        int minute = Integer.parseInt(timeMatcher.group(2));
        if (hour > 23 || minute > 59) return null;

        java.time.LocalDate date;

        if (lower.startsWith("avui") || lower.startsWith("today")) {
            date = java.time.LocalDate.now(ZONE_ES);
        } else if (lower.startsWith("dem") || lower.startsWith("tomorrow")) {
            // "demà" o "dema"
            date = java.time.LocalDate.now(ZONE_ES).plusDays(1);
        } else {
            // Prova DD/MM/YYYY primer (més específic)
            var fullMatcher = PAT_DATE_FULL.matcher(lower);
            if (fullMatcher.find()) {
                int day   = Integer.parseInt(fullMatcher.group(1));
                int month = Integer.parseInt(fullMatcher.group(2));
                int year  = Integer.parseInt(fullMatcher.group(3));
                try { date = java.time.LocalDate.of(year, month, day); }
                catch (Exception e) { return null; }
            } else {
                // Prova DD/MM (any actual)
                var shortMatcher = PAT_DATE_SHORT.matcher(lower);
                if (!shortMatcher.find()) return null;
                int day   = Integer.parseInt(shortMatcher.group(1));
                int month = Integer.parseInt(shortMatcher.group(2));
                int year  = java.time.LocalDate.now(ZONE_ES).getYear();
                try { date = java.time.LocalDate.of(year, month, day); }
                catch (Exception e) { return null; }
            }
        }

        try {
            return date.atTime(hour, minute).atZone(ZONE_ES).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private java.util.List<String> resolveNetworks(Map<String, String> draft) {
        // Vídeo (Reel) i Story només van a IG/FB
        boolean mediaOnly = "REEL".equals(draft.get("postType")) || "STORY".equals(draft.get("postType"));
        var list = new java.util.ArrayList<String>();
        if ("1".equals(draft.get("ig"))) list.add("INSTAGRAM");
        if ("1".equals(draft.get("fb"))) list.add("FACEBOOK");
        if (!mediaOnly && "1".equals(draft.get("gb"))) list.add("GOOGLE_BUSINESS");
        if (!mediaOnly && "1".equals(draft.get("li"))) list.add("LINKEDIN");
        return list;
    }

    private void sendPreview(Long chatId, Map<String, String> draft, String caption) {
        boolean isVideo = "video".equals(draft.get("mediaKind"));
        String preview = "📋 <b>Resum del post:</b>\n"
            + "Xarxes: " + buildNetworkList(draft) + "\n"
            + "Tipus: " + draft.get("postType") + "\n"
            + (draft.containsKey("mediaUrl")
               ? (isVideo ? "🎬 Vídeo: " : "🖼 Imatge: ") + draft.get("mediaUrl") + "\n" : "")
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

        String pt = draft.get("postType");
        boolean needsMedia = "PHOTO".equals(pt) || "REEL".equals(pt);
        if (needsMedia) {
            draft.put("step", "AWAIT_MEDIA");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId, "REEL".equals(pt)
                ? "🎬 Envia el vídeo directament aquí (MP4, màx 20 MB), o una URL pública:"
                : "📸 Envia la foto directament aquí, o una URL pública:\n"
                  + "Exemple: <code>https://cdn.amgdl.com/fotos/foto.jpg</code>\n\n"
                  + "O escriu <code>SENSE_FOTO</code> per publicar sense imatge.");
        } else {
            askWhenToPublish(chatId, draft);
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

    /**
     * Publica un post SCHEDULED existent i n'actualitza l'estat (sense crear registre nou).
     * Cridat de forma síncrona des de SocialSchedulerJob.
     */
    public void publishNow(SocialPost scheduledPost) {
        try {
            publishToNetwork(scheduledPost.getTenantId(), scheduledPost.getNetwork(),
                scheduledPost.getPostType(), scheduledPost.getCaption(), scheduledPost.getMediaUrl());
            scheduledPost.setStatus("PUBLISHED");
            scheduledPost.setPublishedAt(Instant.now());
        } catch (Exception e) {
            log.error("Error publicant post programat {}: {}", scheduledPost.getId(), e.getMessage());
            scheduledPost.setStatus("FAILED");
            scheduledPost.setErrorMessage(e.getMessage());
        }
        postRepository.save(scheduledPost);
    }

    @Async
    public void publishAsync(UUID tenantId, Long chatId, Map<String, String> draft) {
        String postType  = draft.get("postType");
        String caption   = draft.get("caption");
        String mediaUrl  = draft.get("mediaUrl");

        var results = new StringBuilder("📊 <b>Resultats:</b>\n");
        var labels = Map.of(
            "INSTAGRAM", "Instagram", "FACEBOOK", "Facebook",
            "GOOGLE_BUSINESS", "Google Business", "LINKEDIN", "LinkedIn");

        for (String net : resolveNetworks(draft)) {
            try {
                String extId = publishToNetwork(tenantId, net, postType, caption, mediaUrl);
                savePost(tenantId, net, postType, caption, mediaUrl, extId, null, "PUBLISHED");
                results.append("✅ ").append(labels.get(net)).append(" publicat\n");
            } catch (UnsupportedOperationException e) {
                results.append("⚠️ ").append(labels.get(net)).append(": ").append(e.getMessage()).append("\n");
            } catch (Exception e) {
                savePost(tenantId, net, postType, caption, mediaUrl, null, e.getMessage(), "FAILED");
                results.append("❌ ").append(labels.get(net)).append(": ").append(e.getMessage()).append("\n");
            }
        }

        if (chatId != null) {
            telegramBotClient.sendMessage(chatId, results.toString());
        } else {
            log.info("Social publish results (scheduled): {}", results);
        }
    }

    /**
     * Publica a UNA xarxa i retorna l'ID extern.
     * Llança UnsupportedOperationException si la combinació xarxa+tipus no està suportada
     * (p.ex. vídeo a Google Business) — el caller ho tracta com a avís, no com a error.
     */
    private String publishToNetwork(UUID tenantId, String network, String postType,
                                    String caption, String mediaUrl) {
        String pt  = postType != null ? postType : "";
        // UTM per atribució: cada xarxa marca els seus enllaços amb el seu utm_source
        String cap = UtmTagger.tag(caption != null ? caption : "", network);

        switch (network) {
            case "INSTAGRAM" -> {
                var mc = metaConfigRepo.findByTenantId(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Meta no configurat per a aquest tenant"));
                String token = vaultEncryption.decrypt(mc.getPageAccessTokenEncrypted());
                return switch (pt) {
                    case "REEL"  -> instagramPublisher.publishReel(mc.getInstagramAccountId(), token,
                                        requireMedia(mediaUrl), cap);
                    case "STORY" -> instagramPublisher.publishStory(mc.getInstagramAccountId(), token,
                                        requireMedia(mediaUrl), isVideoUrl(mediaUrl));
                    default      -> instagramPublisher.publishFeedPhoto(mc.getInstagramAccountId(), token,
                                        requireMedia(mediaUrl), cap);
                };
            }
            case "FACEBOOK" -> {
                var mc = metaConfigRepo.findByTenantId(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Meta no configurat per a aquest tenant"));
                String token = vaultEncryption.decrypt(mc.getPageAccessTokenEncrypted());
                return switch (pt) {
                    case "REEL"  -> facebookPublisher.publishVideo(mc.getFacebookPageId(), token,
                                        requireMedia(mediaUrl), cap);
                    case "STORY" -> {
                        if (isVideoUrl(mediaUrl)) {
                            throw new UnsupportedOperationException(
                                "les stories de vídeo a Facebook encara no estan suportades (usa una foto)");
                        }
                        yield facebookPublisher.publishPhotoStory(mc.getFacebookPageId(), token,
                                  requireMedia(mediaUrl));
                    }
                    default      -> "PHOTO".equals(pt) && mediaUrl != null
                                    ? facebookPublisher.publishPhoto(mc.getFacebookPageId(), token, mediaUrl, cap)
                                    : facebookPublisher.publishText(mc.getFacebookPageId(), token, cap);
                };
            }
            case "GOOGLE_BUSINESS" -> {
                if ("REEL".equals(pt) || "STORY".equals(pt)) {
                    throw new UnsupportedOperationException("vídeos i stories no disponibles a Google Business");
                }
                var gConfig = googleConfigRepo.findById(tenantId).orElse(null);
                String locationName = gConfig != null ? gConfig.getBusinessLocationId() : null;
                if (locationName == null || locationName.isBlank()) {
                    throw new IllegalStateException("ubicació de Google Business no configurada");
                }
                return switch (pt) {
                    case "OFFER" -> googleBusinessPublisher.publishOffer(tenantId, locationName, cap, mediaUrl);
                    case "EVENT" -> googleBusinessPublisher.publishEvent(tenantId, locationName, cap, cap, mediaUrl);
                    default      -> googleBusinessPublisher.publishWhatsNew(tenantId, locationName, cap, mediaUrl);
                };
            }
            case "LINKEDIN" -> {
                if ("REEL".equals(pt) || "STORY".equals(pt)) {
                    throw new UnsupportedOperationException("vídeos i stories no disponibles a LinkedIn");
                }
                return linkedInPublisher.publishText(tenantId, cap);
            }
            default -> throw new IllegalArgumentException("Xarxa desconeguda: " + network);
        }
    }

    private static String requireMedia(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            throw new IllegalStateException("aquest tipus de post necessita una foto o un vídeo");
        }
        return mediaUrl;
    }

    /** Detecta vídeo per l'extensió dins la part de path de la URL (les URLs signades porten query). */
    private static boolean isVideoUrl(String url) {
        if (url == null) return false;
        String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov");
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
        if (isLinkedInAvailable(tenantId)) sb.append("💼 <b>L</b> — LinkedIn\n");

        if (sb.isEmpty()) sb.append("⚠️ Cap xarxa configurada. Contacta l'administrador.\n");
        return sb.toString();
    }

    private String buildNetworkList(Map<String, String> draft) {
        var list = new StringBuilder();
        if ("1".equals(draft.get("ig"))) list.append("Instagram ");
        if ("1".equals(draft.get("fb"))) list.append("Facebook ");
        if ("1".equals(draft.get("gb"))) list.append("Google Business ");
        if ("1".equals(draft.get("li"))) list.append("LinkedIn ");
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
