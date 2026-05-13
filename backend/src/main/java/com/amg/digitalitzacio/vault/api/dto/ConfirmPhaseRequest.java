package com.amg.digitalitzacio.vault.api.dto;

import java.util.UUID;

public record ConfirmPhaseRequest(
        UUID communicationRequestId,
        UUID phaseId
) {}
