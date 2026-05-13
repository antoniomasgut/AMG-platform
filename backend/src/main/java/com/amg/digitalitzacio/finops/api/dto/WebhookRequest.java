package com.amg.digitalitzacio.finops.api.dto;

public record WebhookRequest(
        String event,
        String holdedInvoiceId,
        String holdedContactId,
        String status
) {}
