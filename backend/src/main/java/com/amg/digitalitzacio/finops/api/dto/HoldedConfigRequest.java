package com.amg.digitalitzacio.finops.api.dto;

import java.util.UUID;

public record HoldedConfigRequest(
        UUID tenantId,
        String apiKeyRef,
        String holdedCompanyId
) {}
