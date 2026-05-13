package com.amg.digitalitzacio.payments.api.dto;

public record WebhookRequest(
        String event,
        String stripeSessionId,
        String paymentIntentId
) {}
