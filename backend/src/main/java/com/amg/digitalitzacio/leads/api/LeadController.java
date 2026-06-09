package com.amg.digitalitzacio.leads.api;

import com.amg.digitalitzacio.leads.api.dto.*;
import com.amg.digitalitzacio.leads.application.ActivityService;
import com.amg.digitalitzacio.leads.application.ConvertLeadService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final ActivityService activityService;
    private final ConvertLeadService convertLeadService;

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

    @PostMapping("/{id}/analyze-notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnalyzeNotesResponse> analyzeNotes(@PathVariable UUID id,
                                                             @Valid @RequestBody AnalyzeNotesRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.analyzeNotes(id, request, principal));
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

    // --- RGPD: dret d'esborrat (Art. 17) ---

    @DeleteMapping("/{id}/purge")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> purgeLeadPersonalData(@PathVariable UUID id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        leadService.purgeLeadPersonalData(id, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/purge-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> purgeAllTenantLeads(@AuthenticationPrincipal UserPrincipal principal) {
        leadService.purgeAllTenantLeadPersonalData(principal);
        return ResponseEntity.noContent().build();
    }

    // --- Neteja massiva de leads antics ---

    @DeleteMapping("/purge")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Integer>> purgeOldLeads(
            @RequestParam(defaultValue = "90") int olderThanDays,
            @AuthenticationPrincipal UserPrincipal principal) {
        int purged = leadService.purgeOldLeads(olderThanDays, principal);
        return ResponseEntity.ok(Map.of("purged", purged));
    }

    // --- RGPD: portabilitat de dades (Art. 20) ---

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeadResponse>> exportLeads(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.exportLeadsForPortability(principal));
    }

    @PostMapping("/outreach")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Integer>> sendOutreach(
            @Valid @RequestBody OutreachRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.sendOutreach(request, principal));
    }

    @PostMapping("/send-template")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Integer>> sendTemplate(
            @Valid @RequestBody SendTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.sendTemplate(request, principal));
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

    @PutMapping("/{leadId}/activities/{activityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActivityResponse> updateActivity(@PathVariable UUID leadId,
                                                           @PathVariable UUID activityId,
                                                           @Valid @RequestBody ActivityRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(activityService.updateActivity(leadId, activityId, request, principal));
    }

    @DeleteMapping("/{leadId}/activities/{activityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID leadId,
                                               @PathVariable UUID activityId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        activityService.deleteActivity(leadId, activityId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{leadId}/convert")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ConvertLeadResult> convertToClient(
            @PathVariable UUID leadId,
            @Valid @RequestBody ConvertLeadRequest req) {
        return ResponseEntity.ok(convertLeadService.convert(leadId, req));
    }
}
