package com.amg.digitalitzacio.vault.api.dto;

import java.util.UUID;

public record RequestInfoRequest(
        String requestType,
        UUID fieldId,
        String customMessage
) {}
