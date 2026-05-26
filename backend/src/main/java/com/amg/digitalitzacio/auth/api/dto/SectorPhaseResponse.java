package com.amg.digitalitzacio.auth.api.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public record SectorPhaseResponse(
        String sector,
        int phaseNumber,
        String name,
        String description,
        String dependencyType,    // BASE | OPTIONAL | REQUIRED
        List<Integer> requiredPhases,
        BigDecimal setupPrice,
        BigDecimal monthlyPrice
) {
    public static List<Integer> parseRequired(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).toList();
    }
}
