package com.amg.digitalitzacio.leads.api.dto;

import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.leads.domain.PipelineStage;

import java.util.Map;

public record LeadStatsResponse(
        long total,
        Map<PipelineStage, Long> byStage,
        Map<LeadSource, Long> bySource,
        double conversionRate
) {}
