package com.amg.digitalitzacio.leads.api.dto;

import java.util.UUID;

public record ConvertLeadResult(
        UUID tenantId,
        String tenantName,
        String stripeCheckoutUrl,
        String goCardlessRedirectUrl,
        String holdedInvoiceId
) {}
