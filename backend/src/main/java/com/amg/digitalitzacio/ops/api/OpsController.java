package com.amg.digitalitzacio.ops.api;

import com.amg.digitalitzacio.ops.api.dto.*;
import com.amg.digitalitzacio.ops.application.OpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OpsController {

    private final OpsService opsService;

    @GetMapping("/api/v1/ops/health")
    public HealthResponse health() {
        return opsService.getHealth();
    }

    @GetMapping("/api/v1/ops/health/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AllHealthResponse allHealth() {
        return opsService.getAllHealth();
    }

    @GetMapping("/api/v1/ops/incidents")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<IncidentResponse> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceName) {
        return opsService.listIncidents(status, serviceName);
    }

    @PutMapping("/api/v1/ops/incidents/{id}/acknowledge")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public IncidentResponse acknowledgeIncident(@PathVariable UUID id) {
        return opsService.acknowledgeIncident(id);
    }

    @PutMapping("/api/v1/ops/incidents/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public IncidentResponse resolveIncident(@PathVariable UUID id) {
        return opsService.resolveIncident(id);
    }

    @GetMapping("/api/v1/ops/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OpsDashboardResponse dashboard() {
        return opsService.getDashboard();
    }

    @GetMapping("/api/v1/ops/logs/{serviceName}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<LogEntryResponse> getLogs(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "100") int limit) {
        return opsService.getLogs(serviceName, limit);
    }
}
