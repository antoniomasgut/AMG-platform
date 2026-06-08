package com.amg.digitalitzacio.vault.api.dto;

import java.util.List;

public record NexeSetupResponse(
    List<String> contractedPhases,
    List<PhaseStatus> phases,
    Prerequisites prerequisites
) {
    public record PhaseStatus(
        String key,
        boolean isActive,
        boolean isConfigured
    ) {}

    public record Prerequisites(
        boolean whatsappConnected,
        String whatsappPhone,
        boolean agentConfigured,
        long landingCount,
        boolean googleCalendarConnected,
        boolean telegramConfigured
    ) {}
}
