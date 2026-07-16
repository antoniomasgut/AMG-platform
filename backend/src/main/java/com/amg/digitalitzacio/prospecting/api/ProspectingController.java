package com.amg.digitalitzacio.prospecting.api;

import com.amg.digitalitzacio.prospecting.api.dto.*;
import com.amg.digitalitzacio.prospecting.application.ProspectingService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

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

    @PostMapping("/prospects/{id}/generate-outreach")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> generateOutreach(@PathVariable UUID id,
                                                @RequestParam(defaultValue = "email") String channel) {
        return Map.of("message", service.generateOutreach(id, channel));
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

    // ── Spec 12 v2 — nous endpoints ──────────────────────────────────────────

    @PostMapping("/prospects/{id}/analyze-web")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ProspectResponse analyzeWeb(@PathVariable UUID id) {
        return service.analyzeWeb(id);
    }

    @PostMapping("/campaigns/{id}/analyze-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Integer> analyzeAllWeb(@PathVariable UUID id) {
        return Map.of("analyzed", service.analyzeAllWeb(id));
    }

    @PostMapping("/prospects/{id}/ai-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ProspectResponse generateAiReport(@PathVariable UUID id) {
        return service.generateAiReport(id);
    }

    @GetMapping("/prospects/{id}/widget-code")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> getWidgetCode(@PathVariable UUID id) {
        var code = service.getWidgetCode(id);
        return Map.of("code", code != null ? code : "");
    }

    @GetMapping("/prospects/{id}/channels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> detectChannels(@PathVariable UUID id) {
        return service.detectChannels(id);
    }

    @GetMapping("/campaigns/{id}/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> campaignDashboard(@PathVariable UUID id) {
        return service.getCampaignDashboard(id);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> globalDashboard() {
        return service.getGlobalDashboard();
    }

    // ── Spec 12 v2.1 — millores ──────────────────────────────────────────────

    @PostMapping("/prospects/{id}/generate-pitch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> generatePitch(@PathVariable UUID id) {
        return Map.of("pitch", service.generatePitch(id));
    }

    @PostMapping("/prospects/{id}/generate-demo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> generateDemo(@PathVariable UUID id) {
        return service.generateDemo(id);
    }

    @PostMapping("/prospects/{id}/send-pitch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, String> sendPitch(@PathVariable UUID id) {
        return service.sendPitch(id);
    }

    @PostMapping(value = "/campaigns/{id}/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Integer>> importCsv(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.importCsv(id, file));
    }

    @GetMapping("/duplicates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<DuplicateGroupResponse> findDuplicates() {
        return service.findDuplicates();
    }

    /** Pixel de tracking 1x1: l'email del pitch inclou aquesta URL per saber si s'ha obert. */
    @GetMapping("/track/{prospectId}/open")
    public ResponseEntity<byte[]> trackOpen(@PathVariable UUID prospectId) {
        service.trackPitchOpen(prospectId);
        // PNG 1x1 transparent (bytes fixos)
        byte[] pixel = new byte[]{
            (byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,
            0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
            0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,
            0x08,0x06,0x00,0x00,0x00,0x1F,0x15,(byte)0xC4,
            (byte)0x89,0x00,0x00,0x00,0x0A,0x49,0x44,0x41,
            0x54,0x78,(byte)0x9C,0x62,0x00,0x01,0x00,0x00,
            0x05,0x00,0x01,0x0D,0x0A,0x2D,(byte)0xB4,0x00,
            0x00,0x00,0x00,0x49,0x45,0x4E,0x44,(byte)0xAE,
            0x42,0x60,(byte)0x82
        };
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", "no-store, no-cache")
                .body(pixel);
    }

    /** Baixa de comunicacions comercials (LSSI art. 21). Enllaçat des del peu de l'email de pitch. */
    @GetMapping("/unsubscribe/{prospectId}")
    public ResponseEntity<String> unsubscribe(@PathVariable UUID prospectId) {
        service.unsubscribe(prospectId);
        String html = """
                <!DOCTYPE html>
                <html lang="ca"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Baixa confirmada — AMG Digitalització</title></head>
                <body style="font-family:Arial,sans-serif;max-width:480px;margin:80px auto;padding:0 20px;text-align:center;color:#222">
                  <h1 style="font-size:22px">Baixa confirmada</h1>
                  <p style="line-height:1.6;color:#555">
                    No rebreu més comunicacions comercials d'AMG Digitalització.
                    El vostre email s'ha afegit a la nostra llista de supressió.
                  </p>
                  <p style="font-size:12px;color:#999">
                    Per a qualsevol qüestió: info@amgdl.com
                  </p>
                </body></html>
                """;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
