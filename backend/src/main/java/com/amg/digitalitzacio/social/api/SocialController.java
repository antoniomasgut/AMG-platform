package com.amg.digitalitzacio.social.api;

import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.application.SocialFeatureService;
import com.amg.digitalitzacio.social.application.SocialFeatureService.SocialFeatures;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
@Tag(name = "Social Publisher", description = "Spec 52 — Publicació multi-xarxa")
public class SocialController {

    private final SocialPostRepository postRepository;
    private final SocialMetaConfigRepository metaConfigRepo;
    private final VaultEncryption vaultEncryption;
    private final SocialFeatureService featureService;
    private final com.amg.digitalitzacio.social.application.LinkedInAuthService linkedInAuthService;
    private final com.amg.digitalitzacio.social.application.LinkedInPublisherService linkedInPublisher;
    private final com.amg.digitalitzacio.social.application.SocialAnalyticsService analyticsService;
    private final com.amg.digitalitzacio.auth.domain.TenantRepository tenantRepository;

    /** Historial de posts per tenant */
    @GetMapping("/tenants/{tenantId}/posts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<SocialPost>> listPosts(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(postRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
    }

    /** Historial per xarxa */
    @GetMapping("/tenants/{tenantId}/posts/{network}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<SocialPost>> listPostsByNetwork(
            @PathVariable UUID tenantId,
            @PathVariable String network) {
        return ResponseEntity.ok(
            postRepository.findByTenantIdAndNetworkOrderByCreatedAtDesc(tenantId, network.toUpperCase())
        );
    }

    /** Cancel·la un post SCHEDULED */
    @DeleteMapping("/tenants/{tenantId}/posts/{postId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> cancelPost(
            @PathVariable UUID tenantId,
            @PathVariable UUID postId) {
        var post = postRepository.findById(postId).orElse(null);
        if (post == null || !post.getTenantId().equals(tenantId)) {
            return ResponseEntity.notFound().build();
        }
        if (!"SCHEDULED".equals(post.getStatus())) {
            return ResponseEntity.badRequest().build();
        }
        post.setStatus("CANCELLED");
        postRepository.save(post);
        return ResponseEntity.noContent().build();
    }

    /** Estat de connexió Meta (Instagram + Facebook) */
    @GetMapping("/tenants/{tenantId}/meta/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> metaStatus(@PathVariable UUID tenantId) {
        var mc = metaConfigRepo.findByTenantId(tenantId);
        if (mc.isEmpty()) {
            return ResponseEntity.ok(Map.of("connected", false));
        }
        var config = mc.get();
        boolean tokenValid = config.getTokenExpiresAt() == null
                || config.getTokenExpiresAt().isAfter(Instant.now());
        var result = new java.util.HashMap<String, Object>();
        result.put("connected",             true);
        result.put("facebookPageId",        config.getFacebookPageId() != null ? config.getFacebookPageId() : "");
        result.put("instagramAccountId",    config.getInstagramAccountId() != null ? config.getInstagramAccountId() : "");
        result.put("pagesManagedPosts",     config.getPagesManagedPostsGranted());
        result.put("igContentPublish",      config.getIgContentPublishGranted());
        result.put("tokenValid",            tokenValid);
        result.put("tokenExpiresAt",        config.getTokenExpiresAt() != null ? config.getTokenExpiresAt().toString() : "");
        result.put("defaultContentLanguage", config.getDefaultContentLanguage() != null ? config.getDefaultContentLanguage() : "ca");
        return ResponseEntity.ok(result);
    }

    /** Desa o actualitza la config Meta del tenant (SUPER_ADMIN) */
    @PutMapping("/tenants/{tenantId}/meta/config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> saveMetaConfig(
            @PathVariable UUID tenantId,
            @RequestBody MetaConfigRequest req) {

        var config = metaConfigRepo.findByTenantId(tenantId)
                .orElse(SocialMetaConfig.builder().tenantId(tenantId).build());

        if (req.facebookPageId()       != null) config.setFacebookPageId(req.facebookPageId());
        if (req.instagramAccountId()   != null) config.setInstagramAccountId(req.instagramAccountId());
        if (req.pageAccessToken()      != null && !req.pageAccessToken().isBlank()) {
            config.setPageAccessTokenEncrypted(vaultEncryption.encrypt(req.pageAccessToken()));
        }
        if (req.tokenExpiresAt()       != null) config.setTokenExpiresAt(req.tokenExpiresAt());
        if (req.pagesManagedPosts()         != null) config.setPagesManagedPostsGranted(req.pagesManagedPosts());
        if (req.igContentPublish()          != null) config.setIgContentPublishGranted(req.igContentPublish());
        if (req.defaultContentLanguage()    != null) config.setDefaultContentLanguage(req.defaultContentLanguage());
        config.setUpdatedAt(Instant.now());

        metaConfigRepo.save(config);
        return ResponseEntity.noContent().build();
    }

    /** Toggles de les extensions socials (Mòdul 55) */
    @GetMapping("/tenants/{tenantId}/features")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Map<String, Object>> getFeatures(
            @PathVariable UUID tenantId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
                com.amg.digitalitzacio.shared.security.UserPrincipal principal) {
        var f = featureService.get(tenantId);
        return ResponseEntity.ok(Map.of(
            "enabled",            featureService.isEnabled(tenantId),
            "commentsToTelegram", f.commentsToTelegram(),
            "weeklyAnalytics",    f.weeklyAnalytics(),
            "aiSuggestions",      f.aiSuggestions(),
            "autoPostReviews",    f.autoPostReviews(),
            "dmsToInbox",         f.dmsToInbox()
        ));
    }

    @PutMapping("/tenants/{tenantId}/features")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Void> updateFeatures(
            @PathVariable UUID tenantId,
            @RequestBody SocialFeatures req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
                com.amg.digitalitzacio.shared.security.UserPrincipal principal) {
        if (!featureService.isEnabled(tenantId)) {
            return ResponseEntity.badRequest().build();
        }
        featureService.update(tenantId, req);
        return ResponseEntity.noContent().build();
    }

    /** Sincronitza mètriques de posts publicats on-demand (P17) */
    @PostMapping("/tenants/{tenantId}/metrics/sync")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> syncMetrics(@PathVariable UUID tenantId) {
        analyticsService.syncMetrics(tenantId);
        return ResponseEntity.noContent().build();
    }

    /** Desa l'idioma per defecte del contingut del tenant (ca/es/en/de) */
    @PutMapping("/tenants/{tenantId}/meta/default-language")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> saveDefaultLanguage(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> body) {
        String lang = body.get("language");
        if (lang == null || !java.util.Set.of("ca", "es", "en", "de").contains(lang)) {
            return ResponseEntity.badRequest().build();
        }
        var config = metaConfigRepo.findByTenantId(tenantId)
                .orElse(SocialMetaConfig.builder().tenantId(tenantId).build());
        config.setDefaultContentLanguage(lang);
        config.setUpdatedAt(Instant.now());
        metaConfigRepo.save(config);
        return ResponseEntity.noContent().build();
    }

    // ─── LinkedIn (Mòdul 56 F4) — només tenant propietari AMG ──────────────────

    /** Genera la URL d'autorització OAuth de LinkedIn (només tenant propietari) */
    @GetMapping("/tenants/{tenantId}/linkedin/authorize")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> linkedInAuthorize(@PathVariable UUID tenantId) {
        if (!isOwnerTenant(tenantId)) {
            return ResponseEntity.status(403).build();
        }
        var res = linkedInAuthService.generateAuthUrl(tenantId);
        return ResponseEntity.ok(Map.of("authUrl", res.authUrl()));
    }

    /** Estat de connexió LinkedIn del tenant */
    @GetMapping("/tenants/{tenantId}/linkedin/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> linkedInStatus(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(Map.of("connected", linkedInPublisher.isConnected(tenantId)));
    }

    private boolean isOwnerTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
            .map(t -> Boolean.TRUE.equals(t.getIsOwner()))
            .orElse(false);
    }

    public record MetaConfigRequest(
        String facebookPageId,
        String instagramAccountId,
        String pageAccessToken,
        Instant tokenExpiresAt,
        Boolean pagesManagedPosts,
        Boolean igContentPublish,
        String defaultContentLanguage
    ) {}
}
