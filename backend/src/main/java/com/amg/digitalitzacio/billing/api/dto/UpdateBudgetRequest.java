package com.amg.digitalitzacio.billing.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateBudgetRequest(UUID profileId, List<UUID> phaseIds, List<UUID> addonIds,
                                   String notes, String clientNotes, List<UUID> discountIds,
                                   LocalDate validUntil,
                                   String recommendation, List<UUID> recommendedPhaseIds,
                                   List<Integer> phaseNumbers,
                                   String sector, String businessSize) {}
