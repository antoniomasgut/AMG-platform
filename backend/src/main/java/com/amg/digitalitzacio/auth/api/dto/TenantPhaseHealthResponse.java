package com.amg.digitalitzacio.auth.api.dto;

import java.util.List;
import java.util.UUID;

// DTO de resposta agregat per a l'estat de salut de totes les fases d'un tenant
public record TenantPhaseHealthResponse(
    UUID tenantId,
    String tenantName,
    List<String> contractedPhases,
    List<PhaseHealthResponse> phaseHealth,
    List<String> prerequisiteWarnings  // p.e. "F3 activa sense F1"
) {}
