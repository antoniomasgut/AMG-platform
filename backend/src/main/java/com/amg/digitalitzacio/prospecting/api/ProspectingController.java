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

import java.util.List;
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
    public List<CampaignResponse> listCampaigns(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String location) {
        return service.listCampaigns(sector, location);
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
}
