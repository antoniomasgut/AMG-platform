package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.content.domain.ContentItemStatus;
import com.amg.digitalitzacio.content.domain.ContentPlanItem;
import com.amg.digitalitzacio.content.domain.ContentPlanItemRepository;
import com.amg.digitalitzacio.social.application.SocialPublisherOrchestrator;
import com.amg.digitalitzacio.social.domain.SocialPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

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

    public ContentPlanItem publishItem(ContentPlanItem item) {
        List<String> networks = parseNetworks(item.getNetworks());
        if (networks.isEmpty()) {
            item.setStatus(ContentItemStatus.FAILED);
            item.setError("Cap xarxa definida");
            return itemRepository.save(item);
        }

        boolean anyFailed = false;
        String firstError = null;
        for (String network : networks) {
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

        if (anyFailed) {
            item.setStatus(ContentItemStatus.FAILED);
            item.setError(firstError != null ? firstError : "Error de publicació");
        } else {
            item.setStatus(ContentItemStatus.PUBLISHED);
            item.setError(null);
        }
        return itemRepository.save(item);
    }

    private List<String> parseNetworks(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
