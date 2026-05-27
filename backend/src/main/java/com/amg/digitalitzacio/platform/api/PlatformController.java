package com.amg.digitalitzacio.platform.api;

import com.amg.digitalitzacio.platform.api.dto.PlatformRecommendation;
import com.amg.digitalitzacio.platform.application.PlatformRecommendationService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformRecommendationService recommendationService;

    /** Recomanacions de connectivitat/qualitat per al client */
    @GetMapping("/recommendations/{tenantId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PlatformRecommendation>> getClientRecommendations(@PathVariable UUID tenantId) {
        var principal = getPrincipal();
        if (principal == null) return ResponseEntity.status(401).build();
        boolean isStaff = "SUPER_ADMIN".equals(principal.role()) || "ADMIN".equals(principal.role());
        if (!isStaff && !tenantId.equals(principal.tenantId())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(recommendationService.forClient(tenantId));
    }

    /** Insights d'upgrade i optimització de cost — només per a admins */
    @GetMapping("/insights/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<PlatformRecommendation>> getAdminInsights(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(recommendationService.forAdmin(tenantId));
    }

    private UserPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) return null;
        return p;
    }
}
