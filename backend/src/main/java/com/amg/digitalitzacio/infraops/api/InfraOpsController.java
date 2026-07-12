package com.amg.digitalitzacio.infraops.api;

import com.amg.digitalitzacio.infraops.api.dto.ContainerStatus;
import com.amg.digitalitzacio.infraops.api.dto.InfraStatusResponse;
import com.amg.digitalitzacio.infraops.api.dto.MetricSnapshotResponse;
import com.amg.digitalitzacio.infraops.api.dto.RecommendationResponse;
import com.amg.digitalitzacio.infraops.application.InfraOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/infraops")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class InfraOpsController {

    private final InfraOpsService infraOpsService;

    @GetMapping("/status")
    public InfraStatusResponse getStatus() {
        return infraOpsService.getStatus();
    }

    @GetMapping("/metrics")
    public Page<MetricSnapshotResponse> getMetrics(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return infraOpsService.getMetrics(hours, page, size);
    }

    @GetMapping("/recommendations")
    public List<RecommendationResponse> getActiveRecommendations() {
        return infraOpsService.getActiveRecommendations();
    }

    @GetMapping("/recommendations/history")
    public List<RecommendationResponse> getRecommendationHistory() {
        return infraOpsService.getRecommendationHistory();
    }

    @PostMapping("/recommendations/{id}/resolve")
    public RecommendationResponse resolveRecommendation(@PathVariable UUID id) {
        return infraOpsService.resolveRecommendation(id);
    }

    @DeleteMapping("/recommendations/{id}")
    public void deleteRecommendation(@PathVariable UUID id) {
        infraOpsService.deleteRecommendation(id);
    }

    @GetMapping("/containers")
    public List<ContainerStatus> getContainers() {
        return infraOpsService.getContainerStatuses();
    }

    @PostMapping("/collect")
    public InfraStatusResponse collectNow() {
        return infraOpsService.collectNow();
    }
}
