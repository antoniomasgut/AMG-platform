package com.amg.digitalitzacio.payments.api.dto;

import java.util.List;

public record PaymentListResponse(
        List<PaymentResponse> payments,
        int page,
        int totalPages,
        long totalElements
) {}
