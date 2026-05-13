package com.amg.digitalitzacio.leads.api.dto;

import jakarta.validation.constraints.NotNull;
import com.amg.digitalitzacio.leads.domain.PipelineStage;

public record StageChangeRequest(
        @NotNull PipelineStage stage,
        String lostReason
) {}
