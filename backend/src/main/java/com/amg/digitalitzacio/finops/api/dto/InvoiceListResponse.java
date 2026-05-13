package com.amg.digitalitzacio.finops.api.dto;

import java.util.List;

public record InvoiceListResponse(
        List<InvoiceResponse> invoices,
        int page,
        int totalPages,
        long totalElements
) {}
