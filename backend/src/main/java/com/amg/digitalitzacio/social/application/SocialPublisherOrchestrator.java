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
        if (tenant != null && tenant.getSector() != null) {
            draft.put("sector", tenant.getSector().name());
        }
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

        // Cancel·lació explícita del flux en qualsevol pas
        if (text != null && text.trim().toLowerCase().matches("/cancel|cancel|cancel·lar|cancelar|sortir|exit")) {
            redis.delete(KEY_PREFIX + chatId);
            telegramBotClient.sendMessage(chatId,
                "❌ Publicació cancel·lada.\nEscriu <code>/publica</code> quan vulguis tornar a publicar.");
            return;
        }

        String step = draft.get("step");

        switch (step) {
            case "AWAIT_NETWORKS"        -> handleNetworks(chatId, text, draft);
            case "AWAIT_TYPE"            -> handleType(chatId, text, draft);
            case "AWAIT_MEDIA"           -> handleMedia(chatId, text, photoFileId, videoFileId, draft);
            case "AWAIT_CAROUSEL_MEDIA"  -> handleCarouselMedia(chatId, text, photoFileId, draft);
            case "AWAIT_LINK_URL"        -> handleLinkUrl(chatId, text, draft);
            case "AWAIT_CAPTION"         -> handleCaption(chatId, text, draft);
            case "AWAIT_SCHEDULE"        -> handleSchedule(chatId, text, draft);
            case "AWAIT_CONFIRM"         -> handleConfirm(chatId, text, draft);
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
            + (ig ? "📸 <code>FOTO</code> — Foto amb caption\n"
                  + "   🖼 <code>CARRUSEL</code> — 2–10 fotos (carrusel)\n"
                  + "   🎬 <code>VIDEO</code> — Reel (vídeo al feed)\n"
                  + "   ⭕ <code>STORY</code> — Story (foto o vídeo, 24h)\n" : "")
            + (fb ? "📝 <code>TEXT</code> — Missatge de text\n"
                  + "   📸 <code>FOTO</code> — Foto amb caption\n"
                  + "   🔗 <code>LINK</code> — Compartir un enllaç\n"
                  + (!ig ? "   🎬 <code>VIDEO</code> — Vídeo a la pàgina\n"
                         + "   ⭕ <code>STORY</code> — Story de foto (24h)\n" : "") : "")
            + (gb ? "🗺 <code>NOTICIES</code> — Notícia\n"
                  + "   🎉 <code>OFERTA</code> — Oferta\n"
                  + "   📷 <code>GALERIA</code> — Foto a la galeria del perfil (SEO local)\n" : "")
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
            case "FOTO"              -> "PHOTO";
            case "TEXT"              -> "TEXT";
            case "VIDEO", "REEL"     -> "REEL";
            case "STORY", "STORIES"  -> "STORY";
            case "CARRUSEL","CAROUSEL"   -> "CAROUSEL";
            case "LINK", "ENLLAS"        -> "LINK";
            case "NOTICIES"              -> "WHATS_NEW";
            case "OFERTA"                -> "OFFER";
            case "EVENT"                 -> "EVENT";
            case "GALERIA","GALLERY"     -> "GALLERY_PHOTO";
            default                      -> null;
        };

        if (postType == null) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Opció no reconeguda. Opcions: <code>FOTO</code>, <code>CARRUSEL</code>, "
                + "<code>TEXT</code>, <code>LINK</code>, <code>VIDEO</code>, <code>STORY</code>, "
                + "<code>NOTICIES</code>, <code>OFERTA</code>, <code>EVENT</code>, <code>GALERIA</code>.");
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

        // Carrusel: Instagram only
        if ("CAROUSEL".equals(postType) && !"1".equals(draft.get("ig"))) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ El carrusel només és compatible amb Instagram. Selecciona Instagram o tria un altre tipus.");
            return;
        }

        // Link: Facebook only
        if ("LINK".equals(postType) && !"1".equals(draft.get("fb"))) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ La publicació d'enllaços només és compatible amb Facebook. Selecciona Facebook o tria un altre tipus.");
            return;
        }

        // Galeria: Google Business only
        if ("GALLERY_PHOTO".equals(postType) && !"1".equals(draft.get("gb"))) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ La foto de galeria és una funció exclusiva de Google Business. Selecciona Google Business o tria un altre tipus.");
            return;
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

        // Carrusel: pas especial per acumular múltiples fotos
        if ("CAROUSEL".equals(postType)) {
            draft.put("step", "AWAIT_CAROUSEL_MEDIA");
            draft.put("carouselCount", "0");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "🖼 <b>Carrusel Instagram</b>\n\n"
                + "Envia les fotos una per una (mínim 2, màxim 10).\n"
                + "Quan hagis enviat totes les imatges escriu <code>LLEST</code>.");
            return;
        }

        // Link: demanem primer la URL
        if ("LINK".equals(postType)) {
            draft.put("step", "AWAIT_LINK_URL");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "🔗 Envia l'URL que vols compartir (ha de començar per https://):");
            return;
        }

        // Galeria: salta caption, va directe a media (la foto és el contingut)
        if ("GALLERY_PHOTO".equals(postType)) {
            draft.put("step", "AWAIT_MEDIA");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "📷 <b>Foto a la galeria del perfil de Google Business</b>\n\n"
                + "Les fotos de galeria apareixen permanentment al perfil i milloren el SEO local.\n\n"
                + "Envia la foto directament aquí o una URL pública:");
            return;
        }

        draft.put("step", "AWAIT_CAPTION");
        saveDraft(chatId, draft);

        telegramBotClient.sendMessage(chatId,
            "✍️ Escriu el text del post o <code>IA</code> per generar-lo automàticament:");
    }

    /** Acumula fotos del carrusel. Cada foto s'afegeix a la llista fins a LLEST o màxim 10. */
    private void handleCarouselMedia(Long chatId, String text, String photoFileId, Map<String, String> draft) {
        if ("LLEST".equalsIgnoreCase(text.trim())) {
            int count = Integer.parseInt(draft.getOrDefault("carouselCount", "0"));
            if (count < 2) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ Necessites almenys 2 fotos per crear un carrusel. "
                    + "Continua enviant fotos o escriu <code>LLEST</code> quan en tinguis prou.");
                return;
            }
            // Passem al text del post
            draft.put("step", "AWAIT_CAPTION");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "✅ " + count + " fotos carregades.\n✍️ Escriu el text del carrusel o <code>IA</code>:");
            return;
        }

        int count = Integer.parseInt(draft.getOrDefault("carouselCount", "0"));
        if (count >= 10) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Màxim 10 fotos per carrusel. Escriu <code>LLEST</code> per continuar.");
            return;
        }

        String url = null;
        if (photoFileId != null) {
            telegramBotClient.sendMessage(chatId, "⏳ Pujant foto " + (count + 1) + "…");
            try {
                UUID tenantId = UUID.fromString(draft.get("tenantId"));
                url = telegramMediaUploadService.downloadAndUpload(photoFileId, tenantId);
            } catch (Exception e) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ No s'ha pogut pujar la foto: " + e.getMessage());
                return;
            }
        } else if (text.trim().startsWith("http")) {
            url = text.trim();
        } else {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Envia una foto o una URL vàlida. Quan acabis escriu <code>LLEST</code>.");
            return;
        }

        count++;
        draft.put("carouselUrl" + count, url);
        draft.put("carouselCount", String.valueOf(count));
        saveDraft(chatId, draft);
        telegramBotClient.sendMessage(chatId,
            "✅ Foto " + count + " afegida."
            + (count < 10 ? " Envia la següent o escriu <code>LLEST</code> per continuar." : " Màxim assolit. Escriu <code>LLEST</code>."));
    }

    /** Demana l'URL per al POST_LINK de Facebook, després passa al caption. */
    private void handleLinkUrl(Long chatId, String text, Map<String, String> draft) {
        String url = text.trim();
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ L'URL ha de començar per https://. Torna a introduir-la.");
            return;
        }
        draft.put("linkUrl", url);
        draft.put("step", "AWAIT_CAPTION");
        saveDraft(chatId, draft);
        telegramBotClient.sendMessage(chatId,
            "✍️ Escriu el text que acompanyarà l'enllaç (o <code>SENSE_TEXT</code> si no en vols):");
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
        String resolvedMedia = resolveMediaUrl(draft);
        for (String net : resolveNetworks(draft)) {
            postRepository.save(SocialPost.builder()
                .tenantId(tenantId)
                .network(net)
                .postType(draft.get("postType"))
                .caption(draft.get("caption"))
                .mediaUrl(resolvedMedia)
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
        String pt = draft.get("postType");
        // Vídeo, Story i Carrusel: IG/FB
        boolean mediaOnly = "REEL".equals(pt) || "STORY".equals(pt) || "CAROUSEL".equals(pt);
        // Link: FB only
        boolean fbOnly = "LINK".equals(pt);
        // Galeria: GB only
        boolean gbOnly = "GALLERY_PHOTO".equals(pt);
        var list = new java.util.ArrayList<String>();
        if (!fbOnly && !gbOnly && "1".equals(draft.get("ig"))) list.add("INSTAGRAM");
        if (!gbOnly && "1".equals(draft.get("fb"))) list.add("FACEBOOK");
        if (!mediaOnly && !fbOnly && "1".equals(draft.get("gb"))) list.add("GOOGLE_BUSINESS");
        if (!mediaOnly && !fbOnly && !gbOnly && "1".equals(draft.get("li"))) list.add("LINKEDIN");
        return list;
    }

    private void sendPreview(Long chatId, Map<String, String> draft, String caption) {
        boolean isVideo = "video".equals(draft.get("mediaKind"));
        String pt = draft.get("postType");
        var sb = new StringBuilder("📋 <b>Resum del post:</b>\n")
            .append("Xarxes: ").append(buildNetworkList(draft)).append("\n")
            .append("Tipus: ").append(pt).append("\n");

        if ("CAROUSEL".equals(pt)) {
            int count = Integer.parseInt(draft.getOrDefault("carouselCount", "0"));
            sb.append("🖼 ").append(count).append(" fotos al carrusel\n");
        } else if ("LINK".equals(pt) && draft.containsKey("linkUrl")) {
            sb.append("🔗 ").append(draft.get("linkUrl")).append("\n");
        } else if (draft.containsKey("mediaUrl")) {
            sb.append(isVideo ? "🎬 Vídeo: " : "🖼 Imatge: ").append(draft.get("mediaUrl")).append("\n");
        }

        if (caption != null && !caption.isBlank()) sb.append("\n").append(caption);
        sb.append("\n\n✅ Escriu <code>SI</code> per publicar o <code>NO</code> per cancel·lar.");
        telegramBotClient.sendMessage(chatId, sb.toString());
    }

    private void handleCaption(Long chatId, String rawText, Map<String, String> draft) {
        String text = "SENSE_TEXT".equalsIgnoreCase(rawText.trim()) ? "" : rawText;
        handleCaptionInternal(chatId, text, draft);
    }

    // Separat per no duplicar lògica — el real handler és aquí
    private void handleCaptionInternal(Long chatId, String text, Map<String, String> draft) {
        String caption;
        if (text.trim().equalsIgnoreCase("IA")) {
            String business = draft.getOrDefault("business", "el negoci");
            String sector = draft.getOrDefault("sector", "general");
            String postType = draft.getOrDefault("postType", "PHOTO");
            boolean ig = "1".equals(draft.get("ig"));
            String network = ig ? "INSTAGRAM" : ("1".equals(draft.get("fb")) ? "FACEBOOK" : "GOOGLE_BUSINESS");
            String brief = "Publicació de tipus " + postType + " per a " + business
                + " (sector: " + sector + ")";
            caption = contentGenerator.generateCaption(network, business + " — " + sector, brief);
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
            String extId = publishToNetwork(scheduledPost.getTenantId(), scheduledPost.getNetwork(),
                scheduledPost.getPostType(), scheduledPost.getCaption(), scheduledPost.getMediaUrl());
            scheduledPost.setExternalPostId(extId);
            scheduledPost.setExternalPostUrl(
                resolvePostUrl(scheduledPost.getTenantId(), scheduledPost.getNetwork(), extId));
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
        String mediaUrl  = resolveMediaUrl(draft);

        var results = new StringBuilder("📊 <b>Resultats:</b>\n");
        var labels = Map.of(
            "INSTAGRAM", "Instagram", "FACEBOOK", "Facebook",
            "GOOGLE_BUSINESS", "Google Business", "LINKEDIN", "LinkedIn");

        for (String net : resolveNetworks(draft)) {
            try {
                String extId = publishToNetwork(tenantId, net, postType, caption, mediaUrl);
                String postUrl = resolvePostUrl(tenantId, net, extId);
                savePost(tenantId, net, postType, caption, mediaUrl, extId, postUrl, null, "PUBLISHED");
                String urlNote = postUrl != null ? "\n🔗 " + postUrl : "";
                results.append("✅ ").append(labels.get(net)).append(" publicat").append(urlNote).append("\n");
            } catch (UnsupportedOperationException e) {
                results.append("⚠️ ").append(labels.get(net)).append(": ").append(e.getMessage()).append("\n");
            } catch (Exception e) {
                savePost(tenantId, net, postType, caption, mediaUrl, null, null, e.getMessage(), "FAILED");
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
                    case "REEL"     -> instagramPublisher.publishReel(mc.getInstagramAccountId(), token,
                                           requireMedia(mediaUrl), cap);
                    case "STORY"    -> instagramPublisher.publishStory(mc.getInstagramAccountId(), token,
                                           requireMedia(mediaUrl), isVideoUrl(mediaUrl));
                    case "CAROUSEL" -> instagramPublisher.publishCarousel(mc.getInstagramAccountId(), token,
                                           java.util.Arrays.asList(requireMedia(mediaUrl).split("\\|")), cap);
                    default         -> instagramPublisher.publishFeedPhoto(mc.getInstagramAccountId(), token,
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
                    case "LINK"  -> facebookPublisher.publishLink(mc.getFacebookPageId(), token,
                                        requireMedia(mediaUrl), cap);
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
                    case "OFFER"         -> googleBusinessPublisher.publishOffer(tenantId, locationName, cap, mediaUrl);
                    case "EVENT"         -> googleBusinessPublisher.publishEvent(tenantId, locationName, cap, cap, mediaUrl);
                    case "GALLERY_PHOTO" -> googleBusinessPublisher.uploadPhotoToGallery(tenantId, locationName,
                                               requireMedia(mediaUrl));
                    default              -> googleBusinessPublisher.publishWhatsNew(tenantId, locationName, cap, mediaUrl);
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

    /**
     * Resol el mediaUrl efectiu del draft:
     * - CAROUSEL: concatena carouselUrl1..N amb "|"
     * - LINK: usa linkUrl
     * - Resta: mediaUrl directe
     */
    private static String resolveMediaUrl(Map<String, String> draft) {
        String pt = draft.get("postType");
        if ("CAROUSEL".equals(pt)) {
            int count = Integer.parseInt(draft.getOrDefault("carouselCount", "0"));
            if (count == 0) return null;
            var urls = new java.util.ArrayList<String>();
            for (int i = 1; i <= count; i++) {
                String u = draft.get("carouselUrl" + i);
                if (u != null) urls.add(u);
            }
            return String.join("|", urls);
        }
        if ("LINK".equals(pt)) return draft.get("linkUrl");
        return draft.get("mediaUrl");
    }

    private static String requireMedia(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            throw new IllegalStateException("aquest tipus de post necessita una foto o un vídeo");
        }
        return mediaUrl;
    }

    /**
     * Construeix l'URL pública del post publicat per mostrar-la al portal.
     * IG: crida al Graph API per obtenir el permalink_url (best-effort).
     * FB: construeix URL a partir del format "pageId_postId".
     * Altres: null.
     */
    private String resolvePostUrl(UUID tenantId, String network, String externalPostId) {
        if (externalPostId == null) return null;
        try {
            if ("INSTAGRAM".equals(network)) {
                var mc = metaConfigRepo.findByTenantId(tenantId).orElse(null);
                if (mc == null || mc.getPageAccessTokenEncrypted() == null) return null;
                String token = vaultEncryption.decrypt(mc.getPageAccessTokenEncrypted());
                var raw = org.springframework.web.client.RestClient.create().get()
                    .uri("https://graph.facebook.com/v22.0/" + externalPostId
                         + "?fields=permalink_url&access_token=" + token)
                    .retrieve().body(String.class);
                if (raw != null) {
                    var node = objectMapper.readTree(raw);
                    String url = node.path("permalink_url").asText(null);
                    if (url != null && !url.isEmpty()) return url;
                }
            } else if ("FACEBOOK".equals(network) && externalPostId.contains("_")) {
                String[] parts = externalPostId.split("_", 2);
                return "https://www.facebook.com/" + parts[0] + "/posts/" + parts[1];
            }
        } catch (Exception e) {
            log.debug("No s'ha pogut obtenir l'URL del post {}: {}", externalPostId, e.getMessage());
        }
        return null;
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
                          String mediaUrl, String extId, String extUrl, String errorMsg, String status) {
        try {
            var post = SocialPost.builder()
                .tenantId(tenantId)
                .network(network)
                .postType(postType)
                .caption(caption)
                .mediaUrl(mediaUrl)
                .externalPostId(extId)
                .externalPostUrl(extUrl)
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
