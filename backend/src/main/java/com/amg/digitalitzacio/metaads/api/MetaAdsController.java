package com.amg.digitalitzacio.metaads.api;

import com.amg.digitalitzacio.metaads.api.dto.CampaignStatsResponse;
import com.amg.digitalitzacio.metaads.api.dto.MetaAdsConfigRequest;
import com.amg.digitalitzacio.metaads.api.dto.MetaAdsConfigResponse;
import com.amg.digitalitzacio.metaads.application.MetaAdsService;
import com.amg.digitalitzacio.metaads.domain.MetaAdsConfigRepository;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meta-ads")
@RequiredArgsConstructor
public class MetaAdsController {

    private final MetaAdsService metaAdsService;
    private final MetaAdsConfigRepository metaAdsConfigRepository;

    @GetMapping("/global/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Long>> getGlobalSummary() {
        long configured = metaAdsConfigRepository.count();
        long enabled    = metaAdsConfigRepository.findByEnabledTrue().size();
        return ResponseEntity.ok(Map.of("configured", configured, "enabled", enabled));
    }

    @GetMapping("/tenants/{tenantId}/config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public ResponseEntity<MetaAdsConfigResponse> getConfig(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(metaAdsService.getConfig(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<MetaAdsConfigResponse> saveConfig(
            @PathVariable UUID tenantId,
            @Valid @RequestBody MetaAdsConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(metaAdsService.saveConfig(tenantId, request));
    }

    @GetMapping("/tenants/{tenantId}/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public ResponseEntity<CampaignStatsResponse> getStats(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(metaAdsService.getStats(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/sync")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<String> sync(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        int saved = metaAdsService.sync(tenantId);
        return ResponseEntity.ok("Sync completat: " + saved + " nous registres");
    }
}
