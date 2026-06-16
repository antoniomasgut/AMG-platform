package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint per consultar l'historial de follow-ups enviats (F4).
 */
@RestController
@RequestMapping("/api/v1/agents/followups")
@RequiredArgsConstructor
public class FollowupHistoryController {

    private final FollowupLogRepository followupLogRepository;

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public List<FollowupLog> getHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return followupLogRepository.findByTenantIdOrderBySentAtDesc(principal.tenantId())
                .stream()
                .limit(50)
                .toList();
    }
}
