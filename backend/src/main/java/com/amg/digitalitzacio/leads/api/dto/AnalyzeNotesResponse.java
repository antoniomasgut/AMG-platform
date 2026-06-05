package com.amg.digitalitzacio.leads.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyzeNotesResponse(
    List<String> painPoints,
    String recommendedSector,
    String recommendedSize,
    BigDecimal setupAmount,
    BigDecimal monthlyAmount,
    String recommendationPitch
) {}
