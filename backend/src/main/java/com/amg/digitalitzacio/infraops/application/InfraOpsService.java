package com.amg.digitalitzacio.infraops.application;

import com.amg.digitalitzacio.infraops.api.dto.InfraStatusResponse;
import com.amg.digitalitzacio.infraops.api.dto.MetricSnapshotResponse;
import com.amg.digitalitzacio.infraops.api.dto.RecommendationResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface InfraOpsService {
    InfraStatusResponse getStatus();
    Page<MetricSnapshotResponse> getMetrics(int hours, int page, int size);
    List<RecommendationResponse> getActiveRecommendations();
    List<RecommendationResponse> getRecommendationHistory();
    RecommendationResponse resolveRecommendation(UUID id);
    InfraStatusResponse collectNow();
}
