package com.amg.digitalitzacio.leads.api;

import com.amg.digitalitzacio.leads.api.dto.*;
import com.amg.digitalitzacio.leads.application.ActivityService;
import com.amg.digitalitzacio.leads.application.LeadService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final ActivityService activityService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody LeadRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(request, principal));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LeadResponse>> listLeads(
            Pageable pageable,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.listLeads(pageable, stage, source, assignedTo, search, tenantId, principal));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeadStatsResponse> getStats(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.getLeadStats(principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeadResponse> getLead(@PathVariable UUID id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.getLead(id, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeadResponse> updateLead(@PathVariable UUID id,
                                                    @Valid @RequestBody LeadRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.updateLead(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteLead(@PathVariable UUID id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        leadService.deleteLead(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeadResponse> changeStage(@PathVariable UUID id,
                                                     @Valid @RequestBody StageChangeRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.changeStage(id, request, principal));
    }

    @PatchMapping("/{id}/whatsapp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeadResponse> setWhatsapp(
            @PathVariable UUID id,
            @RequestParam boolean value,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.setWhatsapp(id, value, principal));
    }

    @PostMapping("/outreach")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Integer>> sendOutreach(
            @RequestBody OutreachRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.sendOutreach(request, principal));
    }

    // --- Activities ---

    @GetMapping("/{leadId}/activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ActivityResponse>> getActivities(@PathVariable UUID leadId,
                                                                  Pageable pageable,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(activityService.getActivities(leadId, pageable, principal));
    }

    @PostMapping("/{leadId}/activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActivityResponse> createActivity(@PathVariable UUID leadId,
                                                            @Valid @RequestBody ActivityRequest request,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.createActivity(leadId, request, principal));
    }

    @PatchMapping("/{leadId}/activities/{activityId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActivityResponse> completeActivity(@PathVariable UUID leadId,
                                                              @PathVariable UUID activityId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(activityService.completeActivity(leadId, activityId, principal));
    }
}
