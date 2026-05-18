package com.amg.digitalitzacio.billing.api.dto;

import java.util.UUID;

public record ApplyReferralRequest(
    String code,
    UUID newTenantId
) {}
