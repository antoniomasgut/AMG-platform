package com.amg.digitalitzacio.payments.api.dto;

public record SavedPaymentMethodResponse(
        String paymentMethodId,
        String brand,
        String lastFour,
        Integer expMonth,
        Integer expYear
) {}
