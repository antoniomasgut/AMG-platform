package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ChannelUsageService;
import com.amg.digitalitzacio.agents.application.ChannelUsageService.UsageStatsResponse;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChannelUsageController {

    private final ChannelUsageService channelUsageService;

    @GetMapping("/api/v1/agents/{tenantId}/usage-stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN') or (hasRole('ADMIN') and #principal.tenantId == #tenantId)")
    public ResponseEntity<UsageStatsResponse> getStats(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserPrincipal principal) {

        Instant fromInstant = (from != null) ? Instant.parse(from) : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant toInstant   = (to   != null) ? Instant.parse(to)   : Instant.now();

        return ResponseEntity.ok(channelUsageService.getStats(tenantId, fromInstant, toInstant));
    }

    @GetMapping("/api/v1/agents/usage-stats/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UsageStatsResponse> getGlobalStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        Instant fromInstant = (from != null) ? Instant.parse(from) : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant toInstant   = (to   != null) ? Instant.parse(to)   : Instant.now();

        return ResponseEntity.ok(channelUsageService.getGlobalStats(fromInstant, toInstant));
    }
}
