package com.amg.digitalitzacio.leads.api.dto;

import java.math.BigDecimal;

public record ConvertLeadRequest(
        String tenantName,
        String billingEmail,
        String billingNif,
        String billingPhone,
        String billingAddress,
        String billingCity,
        String sector,
        String businessSize,
        BigDecimal setupAmount,
        BigDecimal monthlyAmount,
        String portalBaseUrl
) {}
