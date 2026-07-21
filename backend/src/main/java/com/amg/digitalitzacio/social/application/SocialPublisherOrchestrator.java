package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.shared.storage.application.TenantStorageService;
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
import java.util.List;
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
    private final SocialAnalyticsService analyticsService;
    private final TenantStorageService tenantStorageService;
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
        // Si hi ha un esborrany pendent, oferir reprendre o descartar (P21)
        if (hasDraft(chatId)) {
            var existing = loadDraft(chatId);
            String prevStep = existing.getOrDefault("step", "AWAIT_NETWORKS");
            if (!"AWAIT_RESUME".equals(prevStep)) {
                existing.put("prevStep", prevStep);
                existing.put("step", "AWAIT_RESUME");
                saveDraft(chatId, existing);
                telegramBotClient.sendMessage(chatId,
                    "⚠️ Tens una publicació pendent (pas: <b>" + stepLabel(prevStep) + "</b>).\n\n"
                    + "<code>CONTINUA</code> — reprendre on ho vas deixar\n"
                    + "<code>DESCARTAR</code> — eliminar i iniciar un post nou");
                return;
            }
        }

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

    private static String stepLabel(String step) {
        return switch (step) {
            case "AWAIT_NETWORKS"       -> "selecció de xarxes";
            case "AWAIT_TYPE"           -> "tipus de post";
            case "AWAIT_MEDIA"          -> "URL de la imatge";
            case "AWAIT_CAROUSEL_MEDIA" -> "fotos del carrusel";
            case "AWAIT_LINK_URL"       -> "URL de l'enllaç";
            case "AWAIT_CAPTION"        -> "caption / text";
            case "AWAIT_CAPTION_PICK"   -> "tria d'opció de caption";
            case "AWAIT_SCHEDULE"       -> "data de publicació";
            case "AWAIT_CONFIRM"        -> "confirmació final";
            default                     -> step;
        };
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
            case "AWAIT_RESUME"          -> handleResume(chatId, text, draft);
            case "AWAIT_NETWORKS"        -> handleNetworks(chatId, text, draft);
            case "AWAIT_TYPE"            -> handleType(chatId, text, draft);
            case "AWAIT_MEDIA"           -> handleMedia(chatId, text, photoFileId, videoFileId, draft);
            case "AWAIT_CAROUSEL_MEDIA"  -> handleCarouselMedia(chatId, text, photoFileId, draft);
            case "AWAIT_LINK_URL"        -> handleLinkUrl(chatId, text, draft);
            case "AWAIT_CAPTION"         -> handleCaption(chatId, text, draft);
            case "AWAIT_CAPTION_PICK"    -> handleCaptionPick(chatId, text, draft);
            case "AWAIT_SCHEDULE"        -> handleSchedule(chatId, text, draft);
            case "AWAIT_CONFIRM"         -> handleConfirm(chatId, text, draft);
            default -> {
                redis.delete(KEY_PREFIX + chatId);
                telegramBotClient.sendMessage(chatId, "❌ Flux cancel·lat. Torna a escriure <code>/publica</code>.");
            }
        }
    }

    // ─── Steps ───────────────────────────────────────────────────────────────

    private void handleResume(Long chatId, String text, Map<String, String> draft) {
        String upper = text.trim().toUpperCase().replace("·", "");
        if (upper.startsWith("CONTIN") || upper.equals("SI") || upper.equals("YES")) {
            String prevStep = draft.getOrDefault("prevStep", "AWAIT_NETWORKS");
            draft.put("step", prevStep);
            draft.remove("prevStep");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "✅ Reprenent el flux — pas: <b>" + stepLabel(prevStep) + "</b>\n"
                + "Continua enviant la resposta per a aquest pas.");
        } else if (upper.startsWith("DESCAR") || upper.equals("NOU") || upper.equals("NO")) {
            redis.delete(KEY_PREFIX + chatId);
            telegramBotClient.sendMessage(chatId,
                "🗑 Esborrany eliminat.\nEscriu <code>/publica</code> per iniciar un nou post.");
        } else {
            telegramBotClient.sendMessage(chatId,
                "Escriu <code>CONTINUA</code> per reprendre o <code>DESCARTAR</code> per eliminar l'esborrany.");
        }
    }

    private void handleNetworks(Long chatId, String text, Map<String, String> draft) {
        String upper = text.toUpperCase().replace(",", " ").trim();

        // P35: drecera "TOTS"/"ALL"/"TOTES" → selecciona totes les xarxes disponibles
        boolean tots = upper.contains("TOTS") || upper.contains("ALL") || upper.contains("TOTES");
        boolean ig, fb, gb, li;
        if (tots) {
            UUID tid = UUID.fromString(draft.get("tenantId"));
            var mc = metaConfigRepo.findByTenantId(tid);
            ig = mc.isPresent() && mc.get().getInstagramAccountId() != null;
            fb = mc.isPresent() && mc.get().getFacebookPageId() != null;
            var gc = googleConfigRepo.findById(tid);
            gb = gc.isPresent() && gc.get().isBusinessEnabled();
            li = isLinkedInAvailable(tid);
            if (!ig && !fb && !gb && !li) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ No tens cap xarxa configurada. Contacta l'administrador per connectar-les.");
                return;
            }
        } else {
            ig = upper.contains("I");
            fb = upper.contains("F");
            gb = upper.contains("G");
            li = upper.contains("L");
        }

        if (!ig && !fb && !gb && !li) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Indica almenys una xarxa: <b>I</b> (Instagram), <b>F</b> (Facebook), <b>G</b> (Google Business)"
                + " o <code>TOTS</code> per seleccionar totes.");
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

        // P44: /imatges — mostra les imatges pujades al storage del tenant
        if (text != null && (text.equalsIgnoreCase("/imatges") || text.equalsIgnoreCase("/images"))
                && fileId == null) {
            sendStorageImagePicker(chatId, draft);
            return;
        }

        // P44: IMATGE#N — l'usuari tria una imatge de la galeria per número
        if (text != null && text.toUpperCase().startsWith("IMATGE#") && fileId == null) {
            handleImagePick(chatId, text, draft);
            return;
        }

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

    // ─── P44: image picker des de l'emmagatzematge ────────────────────────────

    private void sendStorageImagePicker(Long chatId, Map<String, String> draft) {
        UUID tenantId = UUID.fromString(draft.get("tenantId"));
        var images = tenantStorageService.listTenantImages(tenantId, 8);
        if (images.isEmpty()) {
            telegramBotClient.sendMessage(chatId,
                "📭 No tens imatges pujades al teu espai d'emmagatzematge.\n"
                + "Envia una foto directament o una URL pública.");
            return;
        }
        draft.put("imgPickerCount", String.valueOf(images.size()));
        for (int i = 0; i < images.size(); i++) {
            draft.put("imgPicker" + (i + 1), images.get(i).fileId());
        }
        saveDraft(chatId, draft);

        var sb = new StringBuilder("🖼 <b>Tria una imatge del teu espai:</b>\n\n");
        for (int i = 0; i < images.size(); i++) {
            String name = images.get(i).fileName();
            if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
            if (name.length() > 40) name = name.substring(0, 37) + "…";
            sb.append(i + 1).append(". ").append(name).append("\n");
        }
        sb.append("\nEscriu <code>IMATGE#N</code> per triar, o envia una foto/URL directament.");
        telegramBotClient.sendMessage(chatId, sb.toString());
    }

    private void handleImagePick(Long chatId, String text, Map<String, String> draft) {
        int pick;
        try { pick = Integer.parseInt(text.replaceAll("(?i)IMATGE#", "").trim()); }
        catch (NumberFormatException e) { pick = -1; }

        String key = "imgPicker" + pick;
        if (pick < 1 || !draft.containsKey(key)) {
            telegramBotClient.sendMessage(chatId,
                "Escriu <code>IMATGE#N</code> amb el número de la llista, o envia una foto/URL directament.");
            return;
        }
        String fileId = draft.get(key);
        // Neteja claus temporals del picker
        int count = Integer.parseInt(draft.getOrDefault("imgPickerCount", "0"));
        for (int i = 1; i <= count; i++) draft.remove("imgPicker" + i);
        draft.remove("imgPickerCount");

        // Genera URL pública signada (5 min per al flux, suficient)
        UUID tenantId = UUID.fromString(draft.get("tenantId"));
        try {
            var images = tenantStorageService.listTenantImages(tenantId, 8);
            var chosen = images.stream().filter(f -> f.fileId().equals(fileId)).findFirst();
            if (chosen.isEmpty()) {
                telegramBotClient.sendMessage(chatId, "⚠️ No s'ha trobat la imatge. Prova de nou.");
                return;
            }
            // Usa el fileId com a mediaUrl (el publisher resoldrà la URL signada en publicar)
            draft.put("mediaUrl", fileId);
            draft.put("mediaKind", "image");
            draft.put("mediaFromStorage", "true");
            saveDraft(chatId, draft);
            askWhenToPublish(chatId, draft);
        } catch (Exception e) {
            telegramBotClient.sendMessage(chatId, "⚠️ Error accedint a la imatge: " + e.getMessage());
        }
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

        // P40: suggeriment de millor hora si hi ha dades suficients
        String bestTimeHint = "";
        try {
            UUID tenantId = UUID.fromString(draft.get("tenantId"));
            var networks = resolveNetworks(draft);
            String net = networks.isEmpty() ? null : networks.get(0);
            String best = analyticsService.getBestTimeToPost(tenantId, net);
            if (best != null) {
                bestTimeHint = "\n\n📊 <i>La teva millor hora: " + best + "</i>";
            }
        } catch (Exception ignored) {}

        telegramBotClient.sendMessage(chatId,
            "⏰ Quan vols publicar?" + bestTimeHint + "\n\n"
            + "<code>ARA</code> — publicar immediatament\n"
            + "<code>avui a les 22:00</code> — avui a l'hora indicada\n"
            + "<code>demà a les 09:30</code> — demà\n"
            + "<code>divendres a les 18:00</code> — el proper divendres\n"
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
                + "<code>divendres a les 18:00</code>\n"
                + "<code>15/07 18:00</code>\n"
                + "<code>15/07/2026 a les 07:00</code>");
            return;
        }

        if (scheduledAt.isBefore(Instant.now())) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ La data ha de ser futura. Torna a intentar-ho o escriu <code>ARA</code>.");
            return;
        }

        // P31: avís si es programa una Story per a més de 24h (les stories expiren en 24h)
        String postType = draft.get("postType");
        if ("STORY".equals(postType) && scheduledAt.isAfter(Instant.now().plus(java.time.Duration.ofHours(23)))) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Les stories expiren a les 24 hores de publicar-se. "
                + "Programa-la per a avui o demà per garantir que arribi als seguidors.");
        }

        // P32: mostrar preview i demanar confirmació abans de programar (igual que ARA)
        draft.put("scheduledAt", scheduledAt.toString());
        draft.put("step", "AWAIT_CONFIRM");
        saveDraft(chatId, draft);
        sendPreview(chatId, draft, draft.get("caption"));
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
        } else if (lowerContainsDayName(lower)) {
            date = nextWeekday(lower);
            if (date == null) return null;
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

    private static final java.util.Map<String, java.time.DayOfWeek> DAY_NAMES = java.util.Map.of(
        "dilluns",   java.time.DayOfWeek.MONDAY,
        "dimarts",   java.time.DayOfWeek.TUESDAY,
        "dimecres",  java.time.DayOfWeek.WEDNESDAY,
        "dijous",    java.time.DayOfWeek.THURSDAY,
        "divendres", java.time.DayOfWeek.FRIDAY,
        "dissabte",  java.time.DayOfWeek.SATURDAY,
        "diumenge",  java.time.DayOfWeek.SUNDAY
    );

    private static boolean lowerContainsDayName(String lower) {
        return DAY_NAMES.keySet().stream().anyMatch(lower::contains);
    }

    /** Retorna la data del proper dia de la setmana indicat (sempre futur, mai avui). */
    private static java.time.LocalDate nextWeekday(String lower) {
        return DAY_NAMES.entrySet().stream()
            .filter(e -> lower.contains(e.getKey()))
            .findFirst()
            .map(e -> {
                var today = java.time.LocalDate.now(ZONE_ES);
                int delta = (e.getValue().getValue() - today.getDayOfWeek().getValue() + 7) % 7;
                if (delta == 0) delta = 7; // si és avui, la setmana que ve
                return today.plusDays(delta);
            })
            .orElse(null);
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

        // P37: si hi ha captions per-xarxa generats per IA, mostrar-los per separat
        var networks = resolveNetworks(draft);
        boolean hasNetworkCaptions = networks.stream().anyMatch(n -> draft.containsKey(captionKey(n)));
        if (hasNetworkCaptions) {
            for (String net : networks) {
                String nc = draft.getOrDefault(captionKey(net), draft.get("caption"));
                if (nc != null && !nc.isBlank()) {
                    sb.append("\n").append(networkEmoji(net)).append(" <b>")
                      .append(networkLabelShort(net)).append(":</b>\n").append(nc);
                }
            }
        } else if (caption != null && !caption.isBlank()) {
            sb.append("\n").append(caption);
        }

        // P32: si hi ha data programada, mostrar-la al resum
        if (draft.containsKey("scheduledAt")) {
            try {
                Instant schAt = Instant.parse(draft.get("scheduledAt"));
                String formatted = java.time.format.DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE_ES).format(schAt);
                sb.append("\n\n⏰ Programat per al: <b>").append(formatted).append("</b>");
            } catch (Exception ignored) {}
        }
        sb.append("\n\n✅ <code>SI</code> — confirmar · ✏️ <code>EDITAR</code> — canviar el text · 🔄 <code>REGENERAR</code> — nova versió IA · ❌ <code>NO</code> — cancel·lar");
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
            // P39: mostra 3 opcions de caption per triar
            generateAndShowOptions(chatId, draft);
            return;
        } else {
            caption = "SENSE_TEXT".equalsIgnoreCase(text.trim()) ? "" : text.trim();
            // Neteja captions per-xarxa antics quan l'usuari escriu manualment
            clearNetworkCaptions(draft);
        }

        draft.put("caption", caption);

        // P25: avís si el caption supera el límit de la xarxa (no bloqueig, l'usuari decideix)
        if (caption != null && !caption.isBlank()) {
            int len = caption.length();
            String captionWarning = null;
            if ("1".equals(draft.get("ig")) && len > 2200) {
                captionWarning = "⚠️ El caption té " + len + " caràcters. Instagram permet un màxim de 2.200.";
            } else if ("1".equals(draft.get("gb")) && len > 1500) {
                captionWarning = "⚠️ El caption té " + len + " caràcters. Google Business permet un màxim de 1.500.";
            }
            if (captionWarning != null) {
                telegramBotClient.sendMessage(chatId, captionWarning
                    + "\nPots editar-lo escrivint <code>EDITAR</code> a la confirmació, o continuar si no supera el límit.");
            }
        }

        String pt = draft.get("postType");
        boolean needsMedia = "PHOTO".equals(pt) || "REEL".equals(pt);
        if (needsMedia) {
            draft.put("step", "AWAIT_MEDIA");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId, "REEL".equals(pt)
                ? "🎬 Envia el vídeo directament aquí (MP4, màx 20 MB), o una URL pública:"
                : "📸 Envia la foto directament aquí, o una URL pública:\n"
                  + "O escriu <code>/imatges</code> per triar del teu espai d'emmagatzematge.\n"
                  + "O escriu <code>SENSE_FOTO</code> per publicar sense imatge.");
        } else {
            askWhenToPublish(chatId, draft);
        }
    }

    private void handleConfirm(Long chatId, String text, Map<String, String> draft) {
        String normalized = text.trim().toLowerCase().replace("í", "i").replace("é", "e");

        // Editar caption sense cancel·lar (P20)
        if (normalized.matches("editar|edit|edita|modificar|modifica")) {
            draft.put("step", "AWAIT_CAPTION");
            saveDraft(chatId, draft);
            String current = draft.get("caption");
            telegramBotClient.sendMessage(chatId,
                "✏️ Caption actual:\n<i>" + (current != null && !current.isBlank() ? current : "(buit)") + "</i>\n\n"
                + "Escriu el nou caption, o <code>IA</code> per generar-ne un de nou amb IA.\n"
                + "O <code>SENSE_TEXT</code> per publicar sense text.");
            return;
        }

        // P33 + P37: regenerar captions IA (per xarxa) sense cancel·lar el flux
        if (normalized.matches("regenerar|regenera|regenerate")) {
            String newCaption = generateAndStoreCaptions(chatId, draft, false);
            draft.put("caption", newCaption);
            saveDraft(chatId, draft);
            sendPreview(chatId, draft, newCaption);
            return;
        }

        // P46: + HASHTAGS — afegeix els hashtags desats al caption
        if (normalized.matches("\\+\\s*hashtags?|hashtags?\\+|afegir hashtags?")) {
            UUID tenantId46 = UUID.fromString(draft.get("tenantId"));
            String presets = metaConfigRepo.findByTenantId(tenantId46)
                .map(SocialMetaConfig::getHashtagPresets)
                .filter(s -> s != null && !s.isBlank())
                .orElse(null);
            if (presets == null) {
                telegramBotClient.sendMessage(chatId,
                    "⚠️ No tens hashtags desats. Escriu <code>/hashtags</code> per desar-ne.");
            } else {
                String current = draft.getOrDefault("caption", "");
                String updated = (current.isBlank() ? "" : current + "\n") + presets;
                draft.put("caption", updated);
                clearNetworkCaptions(draft);
                saveDraft(chatId, draft);
                sendPreview(chatId, draft, updated);
            }
            return;
        }

        // P47: TRADUIR XX — tradueix el caption a un altre idioma
        if (normalized.startsWith("traduir") || normalized.startsWith("traducir") || normalized.startsWith("translate")) {
            handleTranslate(chatId, normalized, draft);
            return;
        }

        redis.delete(KEY_PREFIX + chatId);

        if (!normalized.matches("si|yes|✅|👍|publicar|confirmar")) {
            telegramBotClient.sendMessage(chatId, "❌ Publicació cancel·lada.");
            return;
        }

        UUID tenantId = UUID.fromString(draft.get("tenantId"));

        // P32: si hi ha data programada, guardar com a SCHEDULED en lloc de publicar ara
        if (draft.containsKey("scheduledAt")) {
            try {
                Instant scheduledAt = Instant.parse(draft.get("scheduledAt"));
                String resolvedMedia = resolveMediaUrl(draft);
                for (String net : resolveNetworks(draft)) {
                    // P37: cada xarxa guarda el seu propi caption
                    String netCaption = resolveNetworkCaption(draft, net);
                    postRepository.save(SocialPost.builder()
                        .tenantId(tenantId)
                        .network(net)
                        .postType(draft.get("postType"))
                        .caption(netCaption)
                        .mediaUrl(resolvedMedia)
                        .status("SCHEDULED")
                        .scheduledAt(scheduledAt)
                        .build());
                }
                String formatted = java.time.format.DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE_ES).format(scheduledAt);
                telegramBotClient.sendMessage(chatId,
                    "✅ Post programat per al <b>" + formatted + "</b>.\n"
                    + "Pots veure'l i cancel·lar-lo des del portal.");
            } catch (Exception e) {
                log.error("Error programant post per al tenant {}: {}", tenantId, e.getMessage());
                telegramBotClient.sendMessage(chatId,
                    "⚠️ Error desant el post programat: " + e.getMessage());
            }
            return;
        }

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
        String postType = draft.get("postType");
        String mediaUrl = resolveMediaUrl(draft);

        var results = new StringBuilder("📊 <b>Resultats:</b>\n");
        var labels = Map.of(
            "INSTAGRAM", "Instagram", "FACEBOOK", "Facebook",
            "GOOGLE_BUSINESS", "Google Business", "LINKEDIN", "LinkedIn");

        for (String net : resolveNetworks(draft)) {
            // P37: usa el caption específic de la xarxa si n'hi ha
            String caption = resolveNetworkCaption(draft, net);
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

    /**
     * P34: envia la llista dels pròxims posts programats per al tenant.
     * Cada post inclou un botó de cancel·lació inline (màxim 5).
     */
    /**
     * P43: vista mensual del calendari de posts programats.
     * Mostra tots els posts programats del mes actual i el mes vinent, agrupats per setmana.
     */
    public void sendMonthlyCalendar(UUID tenantId, Long chatId) {
        var now  = java.time.ZonedDateTime.now(ZONE_ES);
        var from = now.toLocalDate().withDayOfMonth(1).atStartOfDay(ZONE_ES).toInstant();
        var to   = now.toLocalDate().withDayOfMonth(1).plusMonths(2).atStartOfDay(ZONE_ES).toInstant();
        var posts = postRepository.findScheduledBetween(tenantId, from, to);

        if (posts.isEmpty()) {
            telegramBotClient.sendMessage(chatId,
                "📭 No tens cap publicació programada per als pròxims 2 mesos.");
            return;
        }
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZONE_ES);
        var sb  = new StringBuilder("📅 <b>Calendari de publicacions</b>\n\n");
        String lastWeekLabel = null;
        for (var p : posts) {
            var zdt = p.getScheduledAt().atZone(ZONE_ES);
            int weekNum = zdt.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            String weekLabel = "Setmana " + weekNum;
            if (!weekLabel.equals(lastWeekLabel)) {
                if (lastWeekLabel != null) sb.append("\n");
                sb.append("— <b>").append(weekLabel).append("</b> —\n");
                lastWeekLabel = weekLabel;
            }
            String label = NETWORK_LABEL.getOrDefault(p.getNetwork(), p.getNetwork());
            String cap   = p.getCaption() != null && !p.getCaption().isBlank()
                ? (p.getCaption().length() > 45 ? p.getCaption().substring(0, 42) + "…" : p.getCaption())
                : "(sense text)";
            sb.append("• ").append(fmt.format(p.getScheduledAt()))
              .append(" · ").append(label)
              .append(" · \"").append(cap).append("\"\n");
        }
        telegramBotClient.sendMessage(chatId, sb.toString().trim());
    }

    public void sendUpcomingPosts(UUID tenantId, Long chatId) {
        var upcoming = postRepository.findUpcomingScheduled(tenantId, Instant.now());
        if (upcoming.isEmpty()) {
            telegramBotClient.sendMessage(chatId, "📭 No tens cap publicació programada.");
            return;
        }
        int shown = Math.min(upcoming.size(), 5);
        var sb = new StringBuilder("📅 <b>Publicacions programades</b>");
        if (upcoming.size() > 5) sb.append(" (mostrant les primeres 5 de ").append(upcoming.size()).append(")");
        sb.append("\n\n");
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZONE_ES);
        for (int i = 0; i < shown; i++) {
            var p = upcoming.get(i);
            String label = NETWORK_LABEL.getOrDefault(p.getNetwork(), p.getNetwork());
            String when = fmt.format(p.getScheduledAt());
            String caption = p.getCaption() != null && !p.getCaption().isBlank()
                ? (p.getCaption().length() > 40 ? p.getCaption().substring(0, 37) + "…" : p.getCaption())
                : "(sense text)";
            sb.append(i + 1).append(". ").append(label).append(" · ").append(when)
              .append(" · \"").append(caption).append("\"\n")
              .append("   <code>CANCEL·LAR#").append(i + 1).append("</code>\n\n");
        }
        telegramBotClient.sendMessage(chatId,
            sb.append("Escriu <code>CANCEL·LAR#N</code> per cancel·lar una publicació.").toString());
    }

    private static final java.util.Map<String, String> NETWORK_LABEL = java.util.Map.of(
        "INSTAGRAM", "Instagram", "FACEBOOK", "Facebook",
        "GOOGLE_BUSINESS", "Google Business", "LINKEDIN", "LinkedIn");

    /**
     * P34: cancel·la el post programat N-èsim (1-indexed) de la llista upcoming.
     * Verifica que pertany al tenant per seguretat.
     */
    public void cancelUpcomingPost(UUID tenantId, Long chatId, int ordinal) {
        var upcoming = postRepository.findUpcomingScheduled(tenantId, Instant.now());
        if (ordinal < 1 || ordinal > upcoming.size()) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ No existeix el post #" + ordinal + ". Escriu <code>/posts</code> per veure la llista actualitzada.");
            return;
        }
        var post = upcoming.get(ordinal - 1);
        post.setStatus("CANCELLED");
        postRepository.save(post);
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZONE_ES);
        telegramBotClient.sendMessage(chatId,
            "✅ Publicació cancel·lada.\n"
            + "Era: " + NETWORK_LABEL.getOrDefault(post.getNetwork(), post.getNetwork())
            + " · " + fmt.format(post.getScheduledAt()));
    }

    // ─── P46: biblioteca d'hashtags ──────────────────────────────────────────

    /** Mostra els hashtags desats i explica com gestionar-los. */
    public void sendHashtagLibrary(UUID tenantId, Long chatId) {
        String presets = metaConfigRepo.findByTenantId(tenantId)
            .map(SocialMetaConfig::getHashtagPresets)
            .filter(s -> s != null && !s.isBlank())
            .orElse(null);
        if (presets == null) {
            telegramBotClient.sendMessage(chatId,
                "📚 <b>Biblioteca d'hashtags</b>\n\n"
                + "Encara no tens hashtags desats.\n\n"
                + "Per desar-ne, escriu:\n"
                + "<code>DESAR HASHTAGS: #mallorca #restaurant #menú #dinar</code>");
        } else {
            telegramBotClient.sendMessage(chatId,
                "📚 <b>Hashtags desats:</b>\n" + presets + "\n\n"
                + "Per actualitzar-los:\n"
                + "<code>DESAR HASHTAGS: #tag1 #tag2 ...</code>\n"
                + "Durant la confirmació d'un post, escriu <code>+ HASHTAGS</code> per afegir-los.");
        }
    }

    /** Desa (o reemplaza) els hashtags de la biblioteca del tenant. */
    public void saveHashtagPresets(UUID tenantId, Long chatId, String hashtags) {
        var config = metaConfigRepo.findByTenantId(tenantId).orElse(null);
        if (config == null) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ Necessites configurar les xarxes socials primer (Instagram/Facebook).");
            return;
        }
        String cleaned = hashtags.trim();
        config.setHashtagPresets(cleaned);
        config.setUpdatedAt(Instant.now());
        metaConfigRepo.save(config);
        telegramBotClient.sendMessage(chatId,
            "✅ Hashtags desats:\n" + cleaned + "\n\n"
            + "Escriu <code>+ HASHTAGS</code> a la confirmació d'un post per afegir-los.");
    }

    // ─── P41: reutilitzar un post publicat ───────────────────────────────────

    /** Llista els últims 5 posts publicats i ofereix reutilitzar-los. */
    public void sendRecentPublishedPosts(UUID tenantId, Long chatId) {
        var candidates = postRepository.findPublishedSince(tenantId,
                Instant.now().minus(java.time.Duration.ofDays(90)))
            .stream()
            .filter(p -> p.getPublishedAt() != null)
            .collect(java.util.stream.Collectors.toList());

        // P48: si hi ha mètriques, ordena per rendiment; si no, per data
        boolean hasMetrics = candidates.stream()
            .anyMatch(p -> p.getLikes() != null || p.getReach() != null);
        java.util.Comparator<SocialPost> order = hasMetrics
            ? java.util.Comparator.comparingInt(
                (SocialPost p) -> nzPost(p.getReach()) + nzPost(p.getLikes()) * 3 + nzPost(p.getComments()) * 5)
              .reversed()
            : java.util.Comparator.comparing(SocialPost::getPublishedAt).reversed();

        var recent = candidates.stream().sorted(order).limit(5)
            .collect(java.util.stream.Collectors.toList());

        if (recent.isEmpty()) {
            telegramBotClient.sendMessage(chatId,
                "📭 No tens posts publicats recents (últims 90 dies).");
            return;
        }
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM").withZone(ZONE_ES);
        var sb = new StringBuilder("🔄 <b>Posts recents — tria un per reutilitzar</b>\n\n");
        for (int i = 0; i < recent.size(); i++) {
            var p = recent.get(i);
            String label = NETWORK_LABEL.getOrDefault(p.getNetwork(), p.getNetwork());
            String when  = p.getPublishedAt() != null ? fmt.format(p.getPublishedAt()) : "?";
            String cap   = p.getCaption() != null && !p.getCaption().isBlank()
                ? (p.getCaption().length() > 50 ? p.getCaption().substring(0, 47) + "…" : p.getCaption())
                : "(sense text)";
            // P45: mostrar mètriques si disponibles
            String metrics = "";
            if (p.getLikes() != null || p.getReach() != null) {
                metrics = " · " + (p.getLikes() != null ? p.getLikes() + " likes" : "")
                    + (p.getReach() != null ? (p.getLikes() != null ? ", " : "") + p.getReach() + " abast" : "");
            }
            sb.append(i + 1).append(". ").append(label).append(" · ").append(when).append(metrics)
              .append("\n   \"").append(cap).append("\"\n")
              .append("   <code>REUTILITZAR#").append(i + 1).append("</code>\n\n");
        }
        telegramBotClient.sendMessage(chatId, sb.toString().trim());
    }

    /** Inicia un nou flux de publicació pre-emplenat amb les dades del post reutilitzat. */
    public void startReuseFlow(UUID tenantId, Long chatId, int ordinal) {
        var recent = postRepository.findPublishedSince(tenantId,
                Instant.now().minus(java.time.Duration.ofDays(90)))
            .stream()
            .filter(p -> p.getPublishedAt() != null)
            .sorted(java.util.Comparator.comparing(SocialPost::getPublishedAt).reversed())
            .limit(5)
            .collect(java.util.stream.Collectors.toList());

        if (ordinal < 1 || ordinal > recent.size()) {
            telegramBotClient.sendMessage(chatId,
                "⚠️ No existeix el post #" + ordinal + ". Escriu <code>/reutilitzar</code> per veure la llista.");
            return;
        }
        var source = recent.get(ordinal - 1);
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        String businessName = tenant != null ? tenant.getName() : "el teu negoci";

        // Pre-emplena el draft amb les dades del post original
        var draft = new HashMap<String, String>();
        draft.put("tenantId",  tenantId.toString());
        draft.put("business",  businessName);
        draft.put("step",      "AWAIT_CONFIRM");
        draft.put("postType",  source.getPostType() != null ? source.getPostType() : "TEXT");
        draft.put("caption",   source.getCaption() != null ? source.getCaption() : "");
        // Xarxa del post original
        String net = source.getNetwork();
        if ("INSTAGRAM".equals(net))       draft.put("ig", "1");
        else if ("FACEBOOK".equals(net))   draft.put("fb", "1");
        else if ("GOOGLE_BUSINESS".equals(net)) draft.put("gb", "1");
        else if ("LINKEDIN".equals(net))   draft.put("li", "1");
        if (source.getMediaUrl() != null)  draft.put("mediaUrl", source.getMediaUrl());

        saveDraft(chatId, draft);
        telegramBotClient.sendMessage(chatId,
            "🔄 Post pre-emplenat. Pots modificar el caption escrivint <code>EDITAR</code>, "
            + "o confirmar directament.");
        sendPreview(chatId, draft, draft.get("caption"));
    }

    // ─── P47: traducció de caption ───────────────────────────────────────────

    private void handleTranslate(Long chatId, String normalized, Map<String, String> draft) {
        // Format: "traduir ca", "traduir es", "traduir de", "traduir en"
        String langCode = normalized.replaceAll("(traduir|traducir|translate)\\s*", "").trim();
        if (langCode.isEmpty() || !langCode.matches("ca|es|de|en")) {
            telegramBotClient.sendMessage(chatId,
                "🌍 Indica l'idioma de destinació:\n"
                + "<code>TRADUIR CA</code> — català\n"
                + "<code>TRADUIR ES</code> — castellà\n"
                + "<code>TRADUIR DE</code> — alemany\n"
                + "<code>TRADUIR EN</code> — anglès");
            return;
        }
        String caption = draft.get("caption");
        if (caption == null || caption.isBlank()) {
            telegramBotClient.sendMessage(chatId, "⚠️ No hi ha caption per traduir.");
            return;
        }
        telegramBotClient.sendMessage(chatId, "🌍 Traduint…");
        String translated = contentGenerator.translateCaption(caption, langCode);
        draft.put("caption", translated);
        clearNetworkCaptions(draft);
        saveDraft(chatId, draft);
        sendPreview(chatId, draft, translated);
    }

    // ─── P39: tria entre 3 opcions de caption ────────────────────────────────

    /** Genera 3 opcions de caption i presenta-les amb botons per triar. */
    private void generateAndShowOptions(Long chatId, Map<String, String> draft) {
        telegramBotClient.sendMessage(chatId, "🤖 Generant 3 opcions de caption…");

        UUID tenantId = UUID.fromString(draft.get("tenantId"));
        String business = draft.getOrDefault("business", "el negoci");
        String sector   = draft.getOrDefault("sector", "general");
        String postType = draft.getOrDefault("postType", "PHOTO");
        String brief    = "Publicació de tipus " + postType + " per a " + business + " (sector: " + sector + ")";
        String ctx      = business + " — " + sector;

        var networks = resolveNetworks(draft);
        String primaryNet = networks.isEmpty() ? "INSTAGRAM" : networks.get(0);
        List<String> history = recentCaptionsFor(tenantId, primaryNet);

        List<String> options = contentGenerator.generateCaptionOptions(primaryNet, ctx, brief, history);

        for (int i = 0; i < options.size(); i++) {
            draft.put("captionOpt" + (i + 1), options.get(i));
        }
        draft.put("step", "AWAIT_CAPTION_PICK");
        saveDraft(chatId, draft);

        var sb = new StringBuilder("🎨 <b>Tria el caption que més t'agradi:</b>\n\n");
        for (int i = 0; i < options.size(); i++) {
            sb.append("<b>").append(i + 1).append(".</b> ").append(options.get(i)).append("\n\n");
        }
        var numericRow = new java.util.ArrayList<Map<String, String>>();
        for (int i = 1; i <= options.size(); i++) {
            numericRow.add(Map.of("text", String.valueOf(i), "callback_data", String.valueOf(i)));
        }
        telegramBotClient.sendMessageWithRows(chatId, sb.toString().trim(),
            List.of(
                numericRow,
                List.of(
                    Map.of("text", "🔄 Regenerar", "callback_data", "REGENERAR_OPTS"),
                    Map.of("text", "✍️ Escriure", "callback_data", "ESCRIURE")
                )
            ));
    }

    /** Gestiona la tria d'opció de caption (pas AWAIT_CAPTION_PICK). */
    private void handleCaptionPick(Long chatId, String text, Map<String, String> draft) {
        String t = text.trim().toUpperCase();

        if (t.matches("REGENERAR.*|🔄.*|REGENERAR_OPTS")) {
            clearCaptionOptions(draft);
            generateAndShowOptions(chatId, draft);
            return;
        }

        if (t.matches("ESCRIURE|MANUAL|ESCRIU|✍️.*")) {
            clearCaptionOptions(draft);
            draft.put("step", "AWAIT_CAPTION");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId,
                "✏️ Escriu el teu caption, o <code>IA</code> per generar-ne de nous.\n"
                + "O <code>SENSE_TEXT</code> per publicar sense text.");
            return;
        }

        int pick;
        try { pick = Integer.parseInt(t); } catch (NumberFormatException e) { pick = -1; }
        String optKey = (pick >= 1 && pick <= 3) ? "captionOpt" + pick : null;

        if (optKey == null || !draft.containsKey(optKey)) {
            telegramBotClient.sendMessage(chatId,
                "Escriu 1, 2 o 3 per triar una opció, <code>REGENERAR</code> per generar-ne de noves, "
                + "o <code>ESCRIURE</code> per entrar text manualment.");
            return;
        }

        // Opció triada → establir caption i procedir
        String chosen = draft.get(optKey);
        draft.put("caption", chosen);
        clearCaptionOptions(draft);
        // P37: si hi ha múltiples xarxes, genera captions per-xarxa basant-se en l'opció triada
        var networks = resolveNetworks(draft);
        if (networks.size() > 1) {
            generateAndStoreCaptions(chatId, draft, false);
        }

        String pt = draft.get("postType");
        boolean needsMedia = "PHOTO".equals(pt) || "REEL".equals(pt);
        if (needsMedia) {
            draft.put("step", "AWAIT_MEDIA");
            saveDraft(chatId, draft);
            telegramBotClient.sendMessage(chatId, "REEL".equals(pt)
                ? "🎬 Envia el vídeo directament aquí (MP4, màx 20 MB), o una URL pública:"
                : "📸 Envia la foto directament aquí, o una URL pública:\n"
                  + "O escriu <code>SENSE_FOTO</code> per publicar sense imatge.");
        } else {
            askWhenToPublish(chatId, draft);
        }
    }

    private static void clearCaptionOptions(Map<String, String> draft) {
        draft.remove("captionOpt1");
        draft.remove("captionOpt2");
        draft.remove("captionOpt3");
    }

    // ─── Helpers P37 + P38 ────────────────────────────────────────────────────

    /**
     * P37 + P38: genera captions per a cada xarxa seleccionada (o una sola si n'hi ha una).
     * Injecta historial de publicacions recents per evitar repeticions (P38).
     * Desa els captions per-xarxa al draft (captionIG, captionFB...) i retorna el principal.
     * @param notifyUser si true, envia el missatge de "generant..." per Telegram
     */
    private String generateAndStoreCaptions(Long chatId, Map<String, String> draft, boolean notifyUser) {
        UUID tenantId = UUID.fromString(draft.get("tenantId"));
        String business = draft.getOrDefault("business", "el negoci");
        String sector = draft.getOrDefault("sector", "general");
        String postType = draft.getOrDefault("postType", "PHOTO");
        String lang = metaConfigRepo.findByTenantId(tenantId)
                .map(mc -> mc.getDefaultContentLanguage()).filter(l -> l != null && !l.isBlank()).orElse(null);
        String brief = "Publicació de tipus " + postType + " per a " + business
            + " (sector: " + sector + ")"
            + (lang != null ? ". Idioma del caption: " + lang + "." : "");
        String businessCtx = business + " — " + sector;

        var networks = resolveNetworks(draft);
        if (networks.isEmpty()) networks = java.util.List.of("INSTAGRAM");

        if (notifyUser) {
            String genMsg = networks.size() > 1
                ? "🤖 Generant captions adaptats per a cada xarxa…"
                : "🤖 Generant caption…";
            telegramBotClient.sendMessage(chatId, genMsg);
        }

        // Neteja captions antics
        clearNetworkCaptions(draft);

        String primaryCaption = null;
        if (networks.size() == 1) {
            String net = networks.get(0);
            List<String> history = recentCaptionsFor(tenantId, net);
            String nc = contentGenerator.generateCaption(net, businessCtx, brief, history);
            draft.put(captionKey(net), nc);
            primaryCaption = nc;
            telegramBotClient.sendMessage(chatId, "🤖 Caption generat per IA:\n\n" + nc);
        } else {
            var preview = new StringBuilder("🤖 <b>Captions generats per xarxa:</b>\n\n");
            for (String net : networks) {
                List<String> history = recentCaptionsFor(tenantId, net);
                String nc = contentGenerator.generateCaption(net, businessCtx, brief, history);
                draft.put(captionKey(net), nc);
                if (primaryCaption == null) primaryCaption = nc;
                preview.append(networkEmoji(net)).append(" <b>").append(networkLabelShort(net))
                       .append(":</b>\n").append(nc).append("\n\n");
            }
            telegramBotClient.sendMessage(chatId, preview.toString().trim());
        }
        return primaryCaption != null ? primaryCaption : "";
    }

    private static int nzPost(Integer v) { return v != null ? v : 0; }

    private static String captionKey(String network) {
        return switch (network) {
            case "INSTAGRAM"       -> "captionIG";
            case "FACEBOOK"        -> "captionFB";
            case "GOOGLE_BUSINESS" -> "captionGB";
            case "LINKEDIN"        -> "captionLI";
            default -> "caption_" + network.toLowerCase();
        };
    }

    private static String resolveNetworkCaption(Map<String, String> draft, String network) {
        String specific = draft.get(captionKey(network));
        return specific != null ? specific : draft.get("caption");
    }

    private static void clearNetworkCaptions(Map<String, String> draft) {
        draft.remove("captionIG");
        draft.remove("captionFB");
        draft.remove("captionGB");
        draft.remove("captionLI");
    }

    /** P38: últims 5 captions publicats per aquesta xarxa (per context de repeticions). */
    private List<String> recentCaptionsFor(UUID tenantId, String network) {
        try {
            return postRepository.findPublishedSince(tenantId,
                    Instant.now().minus(java.time.Duration.ofDays(30)))
                .stream()
                .filter(p -> network.equals(p.getNetwork()))
                .filter(p -> p.getCaption() != null && !p.getCaption().isBlank())
                .limit(5)
                .map(SocialPost::getCaption)
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String networkEmoji(String network) {
        return switch (network) {
            case "INSTAGRAM"       -> "📸";
            case "FACEBOOK"        -> "📘";
            case "GOOGLE_BUSINESS" -> "🗺";
            case "LINKEDIN"        -> "💼";
            default -> "📱";
        };
    }

    private static String networkLabelShort(String network) {
        return switch (network) {
            case "INSTAGRAM"       -> "Instagram";
            case "FACEBOOK"        -> "Facebook";
            case "GOOGLE_BUSINESS" -> "Google Business";
            case "LINKEDIN"        -> "LinkedIn";
            default -> network;
        };
    }

    /** Reprograma un post FAILED per tornar a intentar en 30 segons (P28). */
    public void requeuePost(java.util.UUID postId) {
        postRepository.findById(postId).ifPresent(post -> {
            if (!"FAILED".equals(post.getStatus())) return;
            post.setStatus("SCHEDULED");
            post.setRetryCount(0);
            post.setScheduledAt(Instant.now().plus(java.time.Duration.ofSeconds(30)));
            post.setErrorMessage(null);
            postRepository.save(post);
        });
    }
}
