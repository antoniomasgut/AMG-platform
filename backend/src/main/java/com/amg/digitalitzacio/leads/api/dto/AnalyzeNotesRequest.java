package com.amg.digitalitzacio.leads.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeNotesRequest(
    @NotBlank(message = "Notes are required for analysis") String notes
) {}
