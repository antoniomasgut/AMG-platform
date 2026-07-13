package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.content.domain.ContentItemStatus;
import com.amg.digitalitzacio.content.domain.ContentPlanItem;
import com.amg.digitalitzacio.content.domain.ContentPlanItemRepository;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.amg.digitalitzacio.social.application.GoogleBusinessPublisherService;
import com.amg.digitalitzacio.social.application.SocialPublisherOrchestrator;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Publica un item del planner a les seves xarxes (Spec 58 §7). Crea un SocialPost per
 * xarxa (amb la referència inversa content_plan_item_id) i reutilitza
 * SocialPublisherOrchestrator.publishNow() — el mateix camí que la publicació programada.
 * L'item passa a PUBLISHED si totes les xarxes van bé, o a FAILED si alguna falla.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ContentPlanPublishService {

    private final SocialPublisherOrchestrator orchestrator;
    private final ContentPlanItemRepository itemRepository;
    private final SocialMetaConfigRepository metaConfigRepository;
    private final GoogleModuleConfigRepository googleConfigRepository;
    private final GoogleBusinessPublisherService googleBusinessPublisher;

    public ContentPlanItem publishItem(ContentPlanItem item) {
        List<String> networks = parseNetworks(item.getNetworks());
        if (networks.isEmpty()) {
            item.setStatus(ContentItemStatus.FAILED);
            item.setError("Cap xarxa definida");
            return itemRepository.save(item);
        }

        // Sense cap destí connectat, publishNow no publicaria res però marcaria PUBLISHED
        // (fals positiu). Ho evitem: SKIPPED amb missatge clar (i el reintent no el toca).
        if (!hasConnectedTarget(item.getTenantId(), networks)) {
            item.setStatus(ContentItemStatus.SKIPPED);
            item.setError("Cap xarxa connectada — connecta Instagram/Facebook o Google Business des del portal.");
            return itemRepository.save(item);
        }

        boolean anyFailed = false;
        String firstError = null;
        for (String network : networks) {
            try {
                if ("GOOGLE_PHOTO".equals(network)) {
                    publishGalleryPhoto(item); // foto a la galeria de Google (no és un post)
                } else {
                    SocialPost post = SocialPost.builder()
                            .tenantId(item.getTenantId())
                            .network(network)
                            .postType("PHOTO")
                            .caption(item.getCaption())
                            .mediaUrl(item.getMediaUrl())
                            .contentPlanItemId(item.getId())
                            .status("DRAFT")
                            .build();
                    orchestrator.publishNow(post); // fixa PUBLISHED/FAILED i desa el SocialPost
                    if (!"PUBLISHED".equals(post.getStatus())) {
                        anyFailed = true;
                        if (firstError == null) firstError = post.getErrorMessage();
                    }
                }
            } catch (Exception e) {
                anyFailed = true;
                if (firstError == null) firstError = e.getMessage();
            }
        }

        if (anyFailed) {
            item.setStatus(ContentItemStatus.FAILED);
            item.setError(firstError != null ? firstError : "Error de publicació");
        } else {
            item.setStatus(ContentItemStatus.PUBLISHED);
            item.setError(null);
        }
        return itemRepository.save(item);
    }

    /** Puja la foto de l'item a la galeria del perfil de Google Business. */
    private void publishGalleryPhoto(ContentPlanItem item) {
        var gc = googleConfigRepository.findById(item.getTenantId()).orElse(null);
        if (gc == null || gc.getBusinessLocationId() == null || gc.getBusinessLocationId().isBlank()) {
            throw new IllegalStateException("Google Business no connectat");
        }
        googleBusinessPublisher.uploadPhotoToGallery(item.getTenantId(), gc.getBusinessLocationId(), item.getMediaUrl());
    }

    /** Cert si el tenant té connectada almenys una de les xarxes de l'item. */
    private boolean hasConnectedTarget(UUID tenantId, List<String> networks) {
        for (String n : networks) {
            switch (n) {
                case "INSTAGRAM", "FACEBOOK" -> {
                    var mc = metaConfigRepository.findByTenantId(tenantId);
                    if (mc.isPresent() && mc.get().getPageAccessTokenEncrypted() != null) return true;
                }
                case "GOOGLE_BUSINESS", "GOOGLE_PHOTO" -> {
                    var gc = googleConfigRepository.findById(tenantId);
                    if (gc.isPresent() && gc.get().getBusinessLocationId() != null
                            && !gc.get().getBusinessLocationId().isBlank()) return true;
                }
                default -> { }
            }
        }
        return false;
    }

    private List<String> parseNetworks(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
