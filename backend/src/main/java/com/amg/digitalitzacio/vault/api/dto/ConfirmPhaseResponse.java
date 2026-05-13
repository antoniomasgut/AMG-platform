package com.amg.digitalitzacio.vault.api.dto;

import java.util.UUID;

public record ConfirmPhaseResponse(
        String phaseStatus,
        NextPhase nextPhase,
        boolean profileCompleted
) {
    public record NextPhase(UUID id, String name, int sortOrder) {}
}
