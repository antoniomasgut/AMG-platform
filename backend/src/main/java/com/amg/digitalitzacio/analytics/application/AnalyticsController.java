package com.amg.digitalitzacio.analytics.application;

import com.amg.digitalitzacio.agents.application.scheduler.ReportScheduler;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.analytics.api.dto.AnalyticsResponse;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportScheduler reportScheduler;
    private final TenantChatLinkRepository chatLinkRepository;

    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(analyticsService.getAnalytics(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/report/send-daily")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<String> sendDailyReport(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        if (chatLink == null || chatLink.getTelegramChatId() == null) {
            return ResponseEntity.badRequest().body("Telegram no configurat");
        }

        // Força l'enviament independent del Redis cooldown (preview manual)
        String text = reportScheduler.buildDailyReportText(tenantId);
        return ResponseEntity.ok(text);
    }
}
