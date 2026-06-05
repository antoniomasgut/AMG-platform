package com.amg.digitalitzacio.metaads.api;

import com.amg.digitalitzacio.metaads.api.dto.*;
import com.amg.digitalitzacio.metaads.application.MetaAdsCampaignService;
import com.amg.digitalitzacio.metaads.application.MetaAdsImageGeneratorService;
import com.amg.digitalitzacio.metaads.application.MetaAdsPublishService;
import com.amg.digitalitzacio.metaads.application.MetaAdsTargetingService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meta-ads")
@RequiredArgsConstructor
public class MetaAdsCampaignController {

    private final MetaAdsCampaignService campaignService;
    private final MetaAdsPublishService publishService;
    private final MetaAdsTargetingService targetingService;
    private final MetaAdsImageGeneratorService imageGeneratorService;

    // ---- Campanyes ----

    @GetMapping("/tenants/{tenantId}/campaigns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public ResponseEntity<List<CampaignResponse>> listCampaigns(
            @PathVariable UUID tenantId, @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(campaignService.listCampaigns(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/campaigns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CampaignResponse> createCampaign(
            @PathVariable UUID tenantId,
            @RequestBody CreateCampaignRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createCampaign(tenantId, req));
    }

    @GetMapping("/tenants/{tenantId}/campaigns/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public ResponseEntity<CampaignResponse> getCampaign(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(campaignService.getCampaign(tenantId, id));
    }

    @PatchMapping("/tenants/{tenantId}/campaigns/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @RequestBody CreateCampaignRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(campaignService.updateCampaign(tenantId, id, req));
    }

    @PostMapping("/tenants/{tenantId}/campaigns/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PublishProgressResponse> publish(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(publishService.publish(tenantId, id));
    }

    @PostMapping("/tenants/{tenantId}/campaigns/{id}/pause")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CampaignResponse> pause(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(publishService.pause(tenantId, id));
    }

    @PostMapping("/tenants/{tenantId}/campaigns/{id}/resume")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CampaignResponse> resume(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(publishService.resume(tenantId, id));
    }

    @PostMapping("/tenants/{tenantId}/campaigns/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CampaignResponse> archive(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(publishService.archive(tenantId, id));
    }

    @PostMapping("/tenants/{tenantId}/campaigns/{id}/duplicate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CampaignResponse> duplicate(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.duplicateCampaign(tenantId, id));
    }

    @DeleteMapping("/tenants/{tenantId}/campaigns/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable UUID tenantId, @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        campaignService.deleteCampaign(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // ---- Ad Sets ----

    @PostMapping("/tenants/{tenantId}/campaigns/{campaignId}/adsets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AdSetResponse> createAdSet(
            @PathVariable UUID tenantId, @PathVariable UUID campaignId,
            @RequestBody CreateAdSetRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createAdSet(tenantId, campaignId, req));
    }

    @PatchMapping("/tenants/{tenantId}/adsets/{adSetId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AdSetResponse> updateAdSet(
            @PathVariable UUID tenantId, @PathVariable UUID adSetId,
            @RequestBody CreateAdSetRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(campaignService.updateAdSet(tenantId, adSetId, req));
    }

    @DeleteMapping("/tenants/{tenantId}/adsets/{adSetId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteAdSet(
            @PathVariable UUID tenantId, @PathVariable UUID adSetId,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        campaignService.deleteAdSet(tenantId, adSetId);
        return ResponseEntity.noContent().build();
    }

    // ---- Ads ----

    @PostMapping("/tenants/{tenantId}/adsets/{adSetId}/ads")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AdResponse> createAd(
            @PathVariable UUID tenantId, @PathVariable UUID adSetId,
            @RequestBody CreateAdRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createAd(tenantId, adSetId, req));
    }

    @DeleteMapping("/tenants/{tenantId}/ads/{adId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteAd(
            @PathVariable UUID tenantId, @PathVariable UUID adId,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        campaignService.deleteAd(tenantId, adId);
        return ResponseEntity.noContent().build();
    }

    // ---- Imatge ----

    @PostMapping("/tenants/{tenantId}/creatives/upload-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @PathVariable UUID tenantId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(targetingService.uploadImage(tenantId, file));
    }

    @PostMapping("/tenants/{tenantId}/creatives/generate-image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ImageUploadResponse> generateImage(
            @PathVariable UUID tenantId,
            @RequestBody GenerateImageRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        try {
            return ResponseEntity.ok(imageGeneratorService.generateAndUpload(tenantId, req));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ---- Targeting ----

    @GetMapping("/tenants/{tenantId}/targeting/interests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<TargetingSearchResult.Item>> searchInterests(
            @PathVariable UUID tenantId, @RequestParam String q,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(targetingService.searchInterests(q, tenantId));
    }

    @GetMapping("/tenants/{tenantId}/targeting/locations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<TargetingSearchResult.Item>> searchLocations(
            @PathVariable UUID tenantId, @RequestParam String q,
            @AuthenticationPrincipal UserPrincipal p) {
        if (!checkAccess(p, tenantId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(targetingService.searchLocations(q, tenantId));
    }

    // ---- Helper ----

    private boolean checkAccess(UserPrincipal p, UUID tenantId) {
        return "SUPER_ADMIN".equals(p.role()) || tenantId.equals(p.tenantId());
    }
}
