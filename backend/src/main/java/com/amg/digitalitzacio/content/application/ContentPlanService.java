package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.assets.application.AssetOrchestrator;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.content.api.dto.ContentPlanResponse;
import com.amg.digitalitzacio.content.api.dto.CreatePlanRequest;
import com.amg.digitalitzacio.content.api.dto.UpdateItemRequest;
import com.amg.digitalitzacio.content.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Servei del Content Planner (Spec 58). Fase 1: CRUD del pla + items, generació per
 * plantilla (pilars), idioma per defecte del tenant i pujada de foto. L'enriquiment IA
 * dels briefs, els schedulers i la publicació arriben a fases posteriors.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ContentPlanService {

    private static final String DEFAULT_NETWORKS = "INSTAGRAM,FACEBOOK,GOOGLE_BUSINESS";
    private static final Set<String> ALLOWED_LANGUAGES = Set.of("ca", "es", "en", "de");
    private static final int WEEKS = 4;

    private final ContentPlanRepository planRepository;
    private final ContentPlanItemRepository itemRepository;
    private final TenantRepository tenantRepository;
    private final SocialMetaConfigRepository metaConfigRepository;
    private final AssetOrchestrator assetOrchestrator;
    private final ContentBriefGenerator briefGenerator;

    // ─────────────────────────── Plans ───────────────────────────

    public ContentPlanResponse createPlan(UUID tenantId, CreatePlanRequest req, UserPrincipal principal) {
        verifyTenantAccess(tenantId, principal);
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant no trobat: " + tenantId);
        }
        String period = req.period();
        if (period == null || !period.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("period ha de tenir el format YYYY-MM");
        }
        planRepository.findByTenantIdAndPeriod(tenantId, period).ifPresent(p -> {
            throw new IllegalArgumentException("Ja existeix un pla per al mes " + period);
        });

        String language = req.contentLanguage() != null && ALLOWED_LANGUAGES.contains(req.contentLanguage())
                ? req.contentLanguage()
                : resolveDefaultLanguage(tenantId);

        ContentPlan plan = ContentPlan.builder()
                .tenantId(tenantId)
                .period(period)
                .status(ContentPlanStatus.DRAFT)
                .contentLanguage(language)
                .createdBy(principal.id())
                .notes(req.notes())
                .build();
        plan = planRepository.save(plan);

        if (Boolean.TRUE.equals(req.generate())) {
            generateItems(plan);
        }
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<ContentPlanResponse> listPlans(UUID tenantId, UserPrincipal principal) {
        verifyTenantAccess(tenantId, principal);
        return planRepository.findByTenantIdOrderByPeriodDesc(tenantId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContentPlanResponse getPlan(UUID planId, UserPrincipal principal) {
        return toResponse(loadOwnedPlan(planId, principal));
    }

    /** (Re)genera els items del pla per plantilla de pilars. */
    public ContentPlanResponse generate(UUID planId, UserPrincipal principal) {
        ContentPlan plan = loadOwnedPlan(planId, principal);
        generateItems(plan);
        return toResponse(plan);
    }

    public ContentPlanResponse activate(UUID planId, UserPrincipal principal) {
        ContentPlan plan = loadOwnedPlan(planId, principal);
        // Un sol pla ACTIVE per tenant: els altres passen a DONE
        planRepository.findByTenantIdAndStatus(plan.getTenantId(), ContentPlanStatus.ACTIVE).forEach(other -> {
            if (!other.getId().equals(plan.getId())) {
                other.setStatus(ContentPlanStatus.DONE);
                planRepository.save(other);
            }
        });
        plan.setStatus(ContentPlanStatus.ACTIVE);
        return toResponse(planRepository.save(plan));
    }

    public void deletePlan(UUID planId, UserPrincipal principal) {
        ContentPlan plan = loadOwnedPlan(planId, principal);
        itemRepository.deleteByPlanId(plan.getId());
        planRepository.delete(plan);
    }

    // ─────────────────────────── Items ───────────────────────────

    public ContentPlanResponse updateItem(UUID itemId, UpdateItemRequest req, UserPrincipal principal) {
        ContentPlanItem item = loadOwnedItem(itemId, principal);
        if (req.pillar() != null) item.setPillar(ContentPillar.valueOf(req.pillar()));
        if (req.briefText() != null) item.setBriefText(req.briefText());
        if (req.exampleText() != null) item.setExampleText(req.exampleText());
        if (req.networks() != null) item.setNetworks(req.networks());
        if (req.contentLanguage() != null) item.setContentLanguage(req.contentLanguage());
        if (req.photoDeadline() != null) item.setPhotoDeadline(req.photoDeadline());
        if (req.targetPublishDate() != null) item.setTargetPublishDate(req.targetPublishDate());
        if (req.status() != null) item.setStatus(ContentItemStatus.valueOf(req.status()));
        itemRepository.save(item);
        return getPlan(item.getPlanId(), principal);
    }

    /** Fotos que falten fer del tenant (items PHOTO_REQUESTED sense foto). */
    @Transactional(readOnly = true)
    public List<com.amg.digitalitzacio.content.api.dto.ContentPlanItemResponse> getPending(UUID tenantId, UserPrincipal principal) {
        verifyTenantAccess(tenantId, principal);
        return itemRepository.findByTenantIdAndStatus(tenantId, ContentItemStatus.PHOTO_REQUESTED).stream()
                .filter(i -> i.getMediaUrl() == null)
                .map(com.amg.digitalitzacio.content.api.dto.ContentPlanItemResponse::from)
                .toList();
    }

    /** El tenant puja la foto d'un item (alternativa al Telegram). */
    public ContentPlanResponse uploadPhoto(UUID itemId, MultipartFile file, UserPrincipal principal) {
        ContentPlanItem item = loadOwnedItem(itemId, principal);
        var asset = assetOrchestrator.upload(file, item.getTenantId());
        item.setMediaUrl(asset.url());
        item.setStatus(ContentItemStatus.PHOTO_RECEIVED);
        itemRepository.save(item);
        return getPlan(item.getPlanId(), principal);
    }

    // ─────────────────────── Idioma per defecte ───────────────────────

    @Transactional(readOnly = true)
    public String getDefaultLanguage(UUID tenantId, UserPrincipal principal) {
        verifyTenantAccess(tenantId, principal);
        return resolveDefaultLanguage(tenantId);
    }

    public String setDefaultLanguage(UUID tenantId, String language, UserPrincipal principal) {
        verifyTenantAccess(tenantId, principal);
        if (language == null || !ALLOWED_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("Idioma no vàlid: " + language + " (ca/es/en/de)");
        }
        SocialMetaConfig config = metaConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> SocialMetaConfig.builder().tenantId(tenantId).build());
        config.setDefaultContentLanguage(language);
        metaConfigRepository.save(config);
        return language;
    }

    // ─────────────────────────── Interns ───────────────────────────

    private void generateItems(ContentPlan plan) {
        itemRepository.deleteByPlanId(plan.getId());
        // Fase 2: briefs adaptats al sector amb IA; fallback a les plantilles del pilar.
        Map<ContentPillar, ContentBriefGenerator.Brief> briefs = generateSectorBriefs(plan.getTenantId());
        LocalDate base = LocalDate.parse(plan.getPeriod() + "-01");
        for (int i = 1; i <= WEEKS; i++) {
            ContentPillar pillar = ContentPillar.ROTATION[(i - 1) % ContentPillar.ROTATION.length];
            ContentBriefGenerator.Brief ai = briefs.get(pillar);
            String briefText = ai != null && ai.brief() != null && !ai.brief().isBlank()
                    ? ai.brief() : pillar.getDefaultBrief();
            String exampleText = ai != null && ai.example() != null && !ai.example().isBlank()
                    ? ai.example() : pillar.getDefaultExample();
            LocalDate weekStart = base.plusWeeks(i - 1);
            LocalDate photoDeadline = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
            ContentPlanItem item = ContentPlanItem.builder()
                    .planId(plan.getId())
                    .tenantId(plan.getTenantId())
                    .weekNumber(i)
                    .pillar(pillar)
                    .briefText(briefText)
                    .exampleText(exampleText)
                    .networks(DEFAULT_NETWORKS)
                    .photoDeadline(photoDeadline)
                    .targetPublishDate(photoDeadline.plusDays(1))
                    .status(ContentItemStatus.PLANNED)
                    .build();
            itemRepository.save(item);
        }
    }

    /** Briefs per sector via IA (buit si no hi ha tenant o la IA falla → s'usen plantilles). */
    private Map<ContentPillar, ContentBriefGenerator.Brief> generateSectorBriefs(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> briefGenerator.generate(
                        t.getName(),
                        t.getSector() != null ? t.getSector().name() : "",
                        "ca"))
                .orElseGet(Map::of);
    }

    private String resolveDefaultLanguage(UUID tenantId) {
        return metaConfigRepository.findByTenantId(tenantId)
                .map(SocialMetaConfig::getDefaultContentLanguage)
                .filter(l -> l != null && !l.isBlank())
                .orElse("ca");
    }

    private ContentPlan loadOwnedPlan(UUID planId, UserPrincipal principal) {
        ContentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Pla no trobat: " + planId));
        assertOwnership(plan.getTenantId(), principal);
        return plan;
    }

    private ContentPlanItem loadOwnedItem(UUID itemId, UserPrincipal principal) {
        ContentPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item no trobat: " + itemId));
        assertOwnership(item.getTenantId(), principal);
        return item;
    }

    /** Aïllament multi-tenant per endpoints sense tenantId a la ruta (Spec 58 §6.3). */
    private void assertOwnership(UUID entityTenantId, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if (principal.tenantId() == null || !principal.tenantId().equals(entityTenantId)) {
            throw new ResourceNotFoundException("Recurs no trobat"); // 404, no filtrem existència cross-tenant
        }
    }

    private void verifyTenantAccess(UUID tenantId, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if (principal.tenantId() == null || !principal.tenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Recurs no trobat");
        }
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "SUPER_ADMIN".equals(principal.role()) || "ADMIN".equals(principal.role());
    }

    private ContentPlanResponse toResponse(ContentPlan plan) {
        return ContentPlanResponse.from(plan, itemRepository.findByPlanIdOrderByWeekNumberAsc(plan.getId()));
    }
}
