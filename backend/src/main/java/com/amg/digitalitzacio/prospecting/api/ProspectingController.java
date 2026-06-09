package com.amg.digitalitzacio.prospecting.api;

import com.amg.digitalitzacio.prospecting.api.dto.*;
import com.amg.digitalitzacio.prospecting.application.ProspectingService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prospecting")
@RequiredArgsConstructor
public class ProspectingController {

    private final ProspectingService service;

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CampaignResponse createCampaign(
            @RequestBody @Valid CreateCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.createCampaign(request, principal.id());
    }

    @GetMapping("/campaigns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Page<CampaignResponse> listCampaigns(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listCampaigns(sector, location, status, page, Math.min(size, 100));
    }

    @GetMapping("/campaigns/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CampaignResponse getCampaign(@PathVariable UUID id) {
        return service.getCampaign(id);
    }

    @PutMapping("/campaigns/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CampaignResponse updateCampaign(
            @PathVariable UUID id,
            @RequestBody UpdateCampaignRequest request) {
        return service.updateCampaign(id, request);
    }

    @DeleteMapping("/campaigns/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public void deleteCampaign(@PathVariable UUID id) {
        service.deleteCampaign(id);
    }

    @PostMapping("/campaigns/{id}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CampaignRunResponse runCampaign(@PathVariable UUID id) {
        return service.runCampaign(id);
    }

    @GetMapping("/campaigns/{id}/prospects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<ProspectResponse> listProspects(
            @PathVariable UUID id,
            @RequestParam(required = false) String status) {
        return service.listProspects(id, status);
    }

    @PutMapping("/prospects/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ProspectResponse updateProspect(
            @PathVariable UUID id,
            @RequestBody UpdateProspectRequest request) {
        return service.updateProspect(id, request);
    }

    @PostMapping("/prospects/{id}/enrich")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ProspectResponse enrichProspect(@PathVariable UUID id) {
        return service.enrichProspect(id);
    }

    @PostMapping("/prospects/{id}/export")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public LeadExportResponse exportProspect(@PathVariable UUID id) {
        return service.exportProspect(id);
    }

    @PostMapping("/campaigns/{id}/export-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public int exportAll(@PathVariable UUID id) {
        return service.exportAllProspects(id);
    }

    @PostMapping("/campaigns/{id}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CampaignResponse cloneCampaign(@PathVariable UUID id) {
        return service.cloneCampaign(id);
    }

    @PostMapping("/campaigns/{id}/enrich-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Integer> enrichAll(@PathVariable UUID id) {
        return Map.of("enriched", service.enrichAllProspects(id));
    }

    @PostMapping("/campaigns/{id}/export-contactable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Integer> exportContactable(@PathVariable UUID id) {
        return service.exportContactableProspects(id);
    }

    @PostMapping("/campaigns/{id}/score")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<ProspectResponse> scoreProspects(@PathVariable UUID id) {
        return service.scoreProspects(id);
    }

    @PostMapping("/campaigns/{id}/qualify-top")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Integer> qualifyTop(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int topN) {
        return Map.of("qualified", service.qualifyTop(id, topN));
    }

    @PostMapping("/campaigns/{id}/qualify-min-score")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Integer> qualifyByMinScore(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "5") int minScore) {
        return Map.of("qualified", service.qualifyByMinScore(id, minScore));
    }

    @PostMapping("/campaigns/{id}/export-qualified")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Integer> exportQualified(@PathVariable UUID id) {
        return service.exportQualifiedProspects(id);
    }

    @PostMapping("/campaigns/{id}/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CampaignResponse scheduleCampaign(
            @PathVariable UUID id,
            @RequestParam Instant nextRun,
            @RequestParam(defaultValue = "7") int repeatDays) {
        return service.scheduleCampaign(id, nextRun, repeatDays);
    }

    @PostMapping("/campaigns/{id}/unschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CampaignResponse unscheduleCampaign(@PathVariable UUID id) {
        return service.unscheduleCampaign(id);
    }
}
